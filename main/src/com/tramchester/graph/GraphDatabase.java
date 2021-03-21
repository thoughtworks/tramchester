package com.tramchester.graph;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.GraphDBConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DataSourceInfo;
import com.tramchester.domain.DataSourceID;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.metrics.TimedTransaction;
import com.tramchester.metrics.Timing;
import com.tramchester.repository.DataSourceRepository;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.neo4j.configuration.ExternalSettings;
import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.configuration.SettingValueParsers;
import org.neo4j.configuration.connectors.BoltConnector;
import org.neo4j.configuration.connectors.HttpConnector;
import org.neo4j.configuration.connectors.HttpsConnector;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.graphalgo.BasicEvaluationContext;
import org.neo4j.graphalgo.EvaluationContext;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.event.DatabaseEventContext;
import org.neo4j.graphdb.event.DatabaseEventListener;
import org.neo4j.graphdb.schema.Schema;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;

@LazySingleton
public class GraphDatabase implements DatabaseEventListener {
    private static final Logger logger = LoggerFactory.getLogger(GraphDatabase.class);
    private static final int SHUTDOWN_TIMEOUT = 200;
    private static final int STARTUP_TIMEOUT = 200;

    private final TramchesterConfig configuration;
    private final DataSourceRepository transportData;
    private final GraphDBConfig graphDBConfig;
    private final String dbName;

    private boolean cleanDB;
    private GraphDatabaseService databaseService;
    private DatabaseManagementService managementService;

    @Inject
    public GraphDatabase(TramchesterConfig configuration, DataSourceRepository transportData) {
        this.configuration = configuration;
        this.transportData = transportData;
        this.graphDBConfig = configuration.getGraphDBConfig();
        dbName = DEFAULT_DATABASE_NAME; // must be this for neo4j community edition default DB
    }

    @PostConstruct
    public void start() {
        logger.info("start");
        Path graphFile = graphDBConfig.getDbPath().toAbsolutePath();

        logger.info("Create or load graph " + graphFile);
        boolean existingFile = Files.exists(graphFile);

        cleanDB = !existingFile;
        if (existingFile) {
            logger.info("Graph db file is present at " + graphFile);
        } else {
            logger.info("No db file found at " + graphFile);
        }

        databaseService = createGraphDatabaseService(graphFile, graphDBConfig);

        if (existingFile && !upToDateVersionsAndNeighbourFlag()) {
            cleanDB = true;
            logger.warn("Graph is out of date, rebuild needed");
            managementService.shutdown();
            int count = 5000 / SHUTDOWN_TIMEOUT; // wait 5 seconds
            while (databaseService.isAvailable(SHUTDOWN_TIMEOUT)) {
                logger.info("Waiting for graph shutdown");
                count--;
                if (count==0) {
                    throw new RuntimeException("Cannot shutdown out of date database");
                }
            }
            try {
                FileUtils.deleteDirectory(graphFile.toFile());
            } catch (IOException e) {
                String message = "Cannot delete out of date graph DB";
                logger.error(message,e);
                throw new RuntimeException(message,e);
            }
            databaseService = createGraphDatabaseService(graphFile, graphDBConfig);
        }

        logger.info("graph db started " + graphFile.toString());
    }

    @PreDestroy
    public void stop() {
        logger.info("stopping");
        stopManagementService();
        logger.info("stopped");
    }

    private void stopManagementService() {
        try (Timing timing = new Timing(logger, "DatabaseManagementService stopping")){
            if (managementService==null) {
                logger.error("Already stopped? Unable to obtain DatabaseManagementService for shutdown");
            } else {
                long begin = System.currentTimeMillis();
                logger.info("Shutting down DatabaseManagementService");
                managementService.shutdown();
                long duration = System.currentTimeMillis() - begin;
                managementService = null;
                databaseService = null;
                logger.info("DatabaseManagementService is shutdown, took " + duration);
            }
        } catch (Exception exceptionInClose) {
            logger.error("DatabaseManagementService Exception during close down " + graphDBConfig.getDbPath(), exceptionInClose);
        }
        logger.info("DatabaseManagementService Stopped");
    }

    public boolean isCleanDB() {
        return cleanDB;
    }

    private boolean upToDateVersionsAndNeighbourFlag() {
        Set<DataSourceInfo> dataSourceInfo = transportData.getDataSourceInfo();
        logger.info("Checking graph version information ");

        // version -> flag
        Map<DataSourceInfo, Boolean> upToDate = new HashMap<>();
        try(Transaction transaction = beginTx()) {

            if (neighboursEnabledMismatch(transaction)) {
                return false;
            }

            List<Node> versionNodes = getNodes(transaction, GraphBuilder.Labels.VERSION);
            if (versionNodes.isEmpty()) {
                logger.warn("Missing VERSION node, cannot check versions");
                return false;
            }
            Node versionNode = versionNodes.get(0);
            Map<String, Object> allProps = versionNode.getAllProperties();

            if (allProps.size()!=dataSourceInfo.size()) {
                logger.warn("VERSION node property mismatch, got " +allProps.size() + " expected " + dataSourceInfo.size());
                return false;
            }

            dataSourceInfo.forEach(sourceInfo -> {
                DataSourceID sourceName = sourceInfo.getID();
                String name = sourceName.getName();
                logger.info("Checking version for " + name);

                if (allProps.containsKey(name)) {
                    String graphValue = allProps.get(name).toString();
                    boolean matches = sourceInfo.getVersion().equals(graphValue);
                    upToDate.put(sourceInfo, matches);
                    if (matches) {
                        logger.info("Got correct VERSION node value for " + sourceInfo);
                    } else {
                        logger.warn(format("Mismatch on graph VERSION, got '%s' for %s", graphValue, sourceInfo));
                    }
                } else {
                    upToDate.put(sourceInfo, false);
                    logger.warn("Could not find version for " + name + " properties were " + allProps.toString());
                }
            });
        }
        return upToDate.values().stream().allMatch(flag -> flag);
    }

    private boolean neighboursEnabledMismatch(Transaction txn) {
        List<Node> nodes = getNodes(txn, GraphBuilder.Labels.NEIGHBOURS_ENABLED);

        boolean dbValue = !nodes.isEmpty(); // presence of node means neighbours present
        boolean configValue = configuration.getCreateNeighbours();

        if (dbValue==configValue) {
            logger.info("CreateNeighbours config matches DB setting of: " + dbValue);
        } else {
            logger.warn("CreateNeighbours config does not match DB setting of: " + dbValue);
        }
        return configValue != dbValue;
    }

    @NotNull
    private List<Node> getNodes(Transaction transaction, GraphBuilder.Labels label) {
        ResourceIterator<Node> query = findNodes(transaction, label);
        List<Node> nodes = query.stream().collect(Collectors.toList());

        if (nodes.size()>1) {
            logger.warn("Too many "+label.name()+ " nodes, will use first");
        }
        return nodes;
    }

    private GraphDatabaseService createGraphDatabaseService(Path graphFile, GraphDBConfig config) {
        logger.info("Create GraphDatabaseService");

        try (Timing timing = new Timing(logger, "DatabaseManagementService build")) {
            managementService = new DatabaseManagementServiceBuilder( graphFile ).
            setConfig(GraphDatabaseSettings.track_query_allocation, false).
            setConfig(GraphDatabaseSettings.store_internal_log_level, Level.WARN ).

            // see https://neo4j.com/docs/operations-manual/current/performance/memory-configuration/#heap-sizing

            setConfig(GraphDatabaseSettings.pagecache_memory, config.getNeo4jPagecacheMemory()).
            setConfig(ExternalSettings.initial_heap_size, "100m").
            setConfig(ExternalSettings.max_heap_size, "200m").
            // TODO This one into config?
            //setConfig(GraphDatabaseSettings.tx_state_max_off_heap_memory, SettingValueParsers.BYTES.parse("256m")).
            setConfig(GraphDatabaseSettings.tx_state_max_off_heap_memory, SettingValueParsers.BYTES.parse("512m")).

            // txn logs, no need to save beyond current ones
            //setConfig(GraphDatabaseSettings.keep_logical_logs, "false").

            // operating in embedded mode
            setConfig(HttpConnector.enabled, false).
            setConfig(HttpsConnector.enabled, false).
            setConfig(BoltConnector.enabled, false).

            // TODO no 4.2 version available?
            //setUserLogProvider(new Slf4jLogProvider()).
            build();
        }

        managementService.registerDatabaseEventListener(this);

        // for community edition must be DEFAULT_DATABASE_NAME
        logger.info("GraphDatabaseService start for " + dbName + " at " + graphDBConfig.getDbPath());
        GraphDatabaseService graphDatabaseService = managementService.database(dbName);

        logger.info("Wait for GraphDatabaseService available");
        int retries = 10;
        while (!graphDatabaseService.isAvailable(STARTUP_TIMEOUT)) {
            logger.error("GraphDatabaseService is not available, name: " + dbName +
                    " Path: " + graphFile.toAbsolutePath() + " check " + retries);
            retries--;
        }

        if (!graphDatabaseService.isAvailable(STARTUP_TIMEOUT)) {
            throw new RuntimeException("Could not start " + graphDBConfig.getDbPath());
        }

        logger.info("GraphDatabaseService is available");
        return graphDatabaseService;
    }

    public Transaction beginTx() {
        return databaseService.beginTx();
    }

    public Transaction beginTx(int timeout, TimeUnit timeUnit) {
        return databaseService.beginTx(timeout, timeUnit);
    }

    public void createIndexs() {

        try (TimedTransaction timed = new TimedTransaction(this, logger, "Create DB Indexes"))
        {
            Transaction tx = timed.transaction();
            Schema schema = tx.schema();

            schema.constraintFor(GraphBuilder.Labels.ROUTE_STATION).
                    assertPropertyIsUnique(GraphPropertyKey.ROUTE_STATION_ID.getText()).create();
            schema.constraintFor(GraphBuilder.Labels.TRAM_STATION).
                    assertPropertyIsUnique(GraphPropertyKey.STATION_ID.getText()).create();
            schema.constraintFor(GraphBuilder.Labels.BUS_STATION).
                    assertPropertyIsUnique(GraphPropertyKey.STATION_ID.getText()).create();
            schema.constraintFor(GraphBuilder.Labels.TRAIN_STATION).
                    assertPropertyIsUnique(GraphPropertyKey.STATION_ID.getText()).create();

            schema.indexFor(GraphBuilder.Labels.ROUTE_STATION).on(GraphPropertyKey.STATION_ID.getText()).create();
            schema.indexFor(GraphBuilder.Labels.ROUTE_STATION).on(GraphPropertyKey.ROUTE_ID.getText()).create();
            schema.indexFor(GraphBuilder.Labels.PLATFORM).on(GraphPropertyKey.PLATFORM_ID.getText()).create();

//            schema.indexFor(GraphBuilder.Labels.SERVICE).on(GraphPropertyKey.SERVICE_ID.getText()).create();

            tx.commit();
        }
    }

    public void waitForIndexesReady(Transaction tx) {
        tx.schema().awaitIndexesOnline(5, TimeUnit.SECONDS);

        tx.schema().getIndexes().forEach(indexDefinition -> logger.info(String.format("Index label %s keys %s",
                indexDefinition.getLabels(), indexDefinition.getPropertyKeys())));
    }

    public Node createNode(Transaction tx, GraphBuilder.Labels label) {
        return tx.createNode(label);
    }

    public Node createNode(Transaction tx, Set<GraphBuilder.Labels> labels) {
        GraphBuilder.Labels[] toApply = new GraphBuilder.Labels[labels.size()];
        labels.toArray(toApply);
        return tx.createNode(toApply);
    }

    public Node findNode(Transaction tx, GraphBuilder.Labels labels, String idField, String idValue) {
        return tx.findNode(labels, idField, idValue);
    }

    public boolean isAvailable(long timeoutMillis) {
        if (databaseService == null) {
            return false;
        }
        return databaseService.isAvailable(timeoutMillis);
    }

    public ResourceIterator<Node> findNodes(Transaction tx, GraphBuilder.Labels label) {
        return tx.findNodes(label);
    }

    public TraversalDescription traversalDescription(Transaction tx) {
        return tx.traversalDescription();
    }


    public EvaluationContext createContext(Transaction txn) {
        return new BasicEvaluationContext(txn, databaseService);
    }


    @Override
    public void databaseStart(DatabaseEventContext eventContext) {
        logger.info("database event: start " + eventContext.getDatabaseName());
    }

    @Override
    public void databaseShutdown(DatabaseEventContext eventContext) {
        logger.warn("database event: shutdown " + eventContext.getDatabaseName());
    }

    @Override
    public void databasePanic(DatabaseEventContext eventContext) {
        logger.error("database event: panic " + eventContext.getDatabaseName());
    }

    public String getDbPath() {
        return graphDBConfig.getDbPath().toAbsolutePath().toString();
    }
}
