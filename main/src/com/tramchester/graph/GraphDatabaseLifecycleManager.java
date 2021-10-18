package com.tramchester.graph;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.GraphDBConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.DataSourceInfo;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.search.GraphDatabaseServiceFactory;
import com.tramchester.metrics.Timing;
import org.apache.commons.io.FileUtils;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;

@LazySingleton
public class GraphDatabaseLifecycleManager {
    private static final Logger logger = LoggerFactory.getLogger(GraphDatabaseLifecycleManager.class);

    private static final int SHUTDOWN_TIMEOUT = 200;

    private final GraphDBConfig graphDBConfig;
    private final TramchesterConfig configuration;
    private final GraphDatabaseServiceFactory serviceFactory;

    private boolean cleanDB;

    @Inject
    public GraphDatabaseLifecycleManager(TramchesterConfig configuration, GraphDatabaseServiceFactory serviceFactory) {
        this.graphDBConfig = configuration.getGraphDBConfig();
        this.configuration = configuration;
        this.serviceFactory = serviceFactory;
        //String dbName = DEFAULT_DATABASE_NAME; // must be this for neo4j community edition default DB
    }

    public GraphDatabaseService startDatabase(Set<DataSourceInfo> dataSourceInfo) {
        Path graphFile = graphDBConfig.getDbPath().toAbsolutePath();

        logger.info("Create or load graph " + graphFile);
        boolean existingFile = Files.exists(graphFile);

        cleanDB = !existingFile;
        if (existingFile) {
            logger.info("Graph db file is present at " + graphFile);
        } else {
            logger.info("No db file found at " + graphFile);
        }

        GraphDatabaseService databaseService = serviceFactory.create();

        if (existingFile && !upToDateVersionsAndNeighbourFlag(databaseService, dataSourceInfo)) {
            cleanDB = true;
            logger.warn("Graph is out of date, rebuild needed");
            serviceFactory.shutdownDatabase();
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
            databaseService = serviceFactory.create();
        }

        logger.info("graph db started at:" + graphFile);

        return databaseService;
    }

    public void stopDatabase() {
        try (Timing ignored = new Timing(logger, "DatabaseManagementService stopping")){
            serviceFactory.shutdownDatabase();

//            if (managementService==null) {
//                logger.error("Already stopped? Unable to obtain DatabaseManagementService for shutdown");
//            } else {
//                logger.info("Shutting down Database");
//                serviceFactory.shutdownDatabase();
//                managementService.shutdown();
//                managementService = null;
//                databaseService = null;
//            }

        } catch (Exception exceptionInClose) {
            logger.error("DatabaseManagementService Exception during close down " + graphDBConfig.getDbPath(), exceptionInClose);
        }
        logger.info("DatabaseManagementService Stopped");
    }

    private boolean upToDateVersionsAndNeighbourFlag(GraphDatabaseService databaseService, Set<DataSourceInfo> dataSourceInfo) {
        logger.info("Checking graph version information ");

        // version -> flag
        Map<DataSourceInfo, Boolean> upToDate = new HashMap<>();
        try(Transaction transaction = databaseService.beginTx()) {

            if (neighboursEnabledMismatch(transaction)) {
                return false;
            }

            List<Node> versionNodes = getNodes(transaction, GraphLabel.VERSION);
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
                String name = sourceName.name();
                logger.info("Checking version for " + sourceName);

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
                    logger.warn("Could not find version for " + name + " properties were " + allProps);
                }
            });
        }
        return upToDate.values().stream().allMatch(flag -> flag);
    }

    private boolean neighboursEnabledMismatch(Transaction txn) {
        List<Node> nodes = getNodes(txn, GraphLabel.NEIGHBOURS_ENABLED);

        boolean dbValue = !nodes.isEmpty(); // presence of node means neighbours present
        boolean configValue = configuration.getCreateNeighbours();

        if (dbValue==configValue) {
            logger.info("CreateNeighbours config matches DB setting of: " + dbValue);
        } else {
            logger.warn("CreateNeighbours config does not match DB setting of: " + dbValue);
        }
        return configValue != dbValue;
    }

    private List<Node> getNodes(Transaction transaction, GraphLabel label) {
        ResourceIterator<Node> query = transaction.findNodes(label); // findNodes(transaction, label);
        List<Node> nodes = query.stream().collect(Collectors.toList());

        if (nodes.size()>1) {
            logger.warn("Too many "+label.name()+ " nodes, will use first");
        }
        return nodes;
    }

    public boolean isCleanDB() {
        return cleanDB;
    }

}
