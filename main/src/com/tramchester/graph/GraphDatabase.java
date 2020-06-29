package com.tramchester.graph;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.graph.graphbuild.GraphBuilder;
import org.apache.commons.io.FileUtils;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.graphalgo.BasicEvaluationContext;
import org.neo4j.graphalgo.EvaluationContext;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.schema.Schema;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.logging.slf4j.Slf4jLogProvider;
import org.picocontainer.Startable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;

public class GraphDatabase implements Startable {
    private static final Logger logger = LoggerFactory.getLogger(GraphDatabase.class);

    private final TramchesterConfig configuration;
    private GraphDatabaseService databaseService;
    private DatabaseManagementService managementService;

    public GraphDatabase(TramchesterConfig configuration) {
        this.configuration = configuration;
    }

    @Override
    public void start() {
        logger.info("start");

        String graphName = configuration.getGraphName();
        logger.info("Create or load graph " + graphName);
        File graphFile = new File(graphName);

        boolean rebuildGraph = configuration.getRebuildGraph();

        if (rebuildGraph) {
            logger.info("Deleting previous graph db for " + graphFile.getAbsolutePath());
            try {
                FileUtils.deleteDirectory(graphFile);
            } catch (IOException ioException) {
                logger.error("Error deleting the graph!", ioException);
            }
        }

        databaseService = createGraphDatabaseService(graphFile);

        logger.info("graph db started " + graphFile.getAbsolutePath());
    }

    private GraphDatabaseService createGraphDatabaseService(File graphFile) {

        managementService = new DatabaseManagementServiceBuilder( graphFile ).
                loadPropertiesFromFile("config/neo4j.conf").
                setUserLogProvider(new Slf4jLogProvider()).
                build();

        // for community edition must be DEFAULT_DATABASE_NAME
        GraphDatabaseService graphDatabaseService = managementService.database(DEFAULT_DATABASE_NAME);

        if (!graphDatabaseService.isAvailable(1000)) {
            logger.error("DB Service is not available, name: " + DEFAULT_DATABASE_NAME +
                    " Path: " + graphFile.toPath().toAbsolutePath());
        }
        return graphDatabaseService;
    }

    @Override
    public void stop() {
        try {
            if (databaseService ==null) {
                logger.error("Unable to obtain GraphDatabaseService for shutdown");
            } else {
                if (databaseService.isAvailable(1000)) {
                    logger.info("Shutting down graphDB");
                    managementService.shutdown();
                    logger.info("graphDB is shutdown");
                } else {
                    logger.warn("Graph reported unavailable, attempt shutdown anyway");
                    managementService.shutdown();
                }
            }
        } catch (Exception exceptionInClose) {
            logger.error("Exception during close down", exceptionInClose);
        }
    }

    public Transaction beginTx() {
        return databaseService.beginTx();
    }

    public Transaction beginTx(int timeout, TimeUnit timeUnit) {
        return databaseService.beginTx(timeout, timeUnit);
    }

    public void createIndexs() {
        logger.info("Create DB indexes");
        try ( Transaction tx = databaseService.beginTx() )
        {
            Schema schema = tx.schema();
            schema.indexFor(GraphBuilder.Labels.TRAM_STATION).on(GraphStaticKeys.ID).create();
            schema.indexFor(GraphBuilder.Labels.BUS_STATION).on(GraphStaticKeys.ID).create();
            schema.indexFor(GraphBuilder.Labels.ROUTE_STATION).on(GraphStaticKeys.ID).create();
            schema.indexFor(GraphBuilder.Labels.PLATFORM).on(GraphStaticKeys.ID).create();
            schema.indexFor(GraphBuilder.Labels.SERVICE).on(GraphStaticKeys.ID).create();
            schema.indexFor(GraphBuilder.Labels.HOUR).on(GraphStaticKeys.ID).create();
            schema.indexFor(GraphBuilder.Labels.MINUTE).on(GraphStaticKeys.ID).create();

            // doesn't help graph build performance....
//            schema.indexFor(TransportRelationshipTypes.TO_SERVICE).on(GraphStaticKeys.TRIPS).
//                    withIndexType(IndexType.FULLTEXT).create();

            tx.commit();
        }
    }

    public void waitForIndexesReady(Transaction tx) {
        tx.schema().awaitIndexesOnline(5, TimeUnit.SECONDS);

        tx.schema().getIndexes().forEach(indexDefinition -> {
            logger.info(String.format("Index label %s keys %s",
                    indexDefinition.getLabels(), indexDefinition.getPropertyKeys()));
        });
    }

    public Node createNode(Transaction tx, GraphBuilder.Labels label) {
        return tx.createNode(label);
    }

    public Node findNode(Transaction tx, GraphBuilder.Labels labels, String idField, String idValue) {
        return tx.findNode(labels, idField, idValue);
    }

    public boolean isAvailable(int timeoutMilli) {
        return databaseService.isAvailable(timeoutMilli);
    }

    public ResourceIterator<Node> findNodes(Transaction tx, GraphBuilder.Labels label) {
        return tx.findNodes(label);
    }

    public TraversalDescription traversalDescription(Transaction tx) {
        return tx.traversalDescription();
    }

    public boolean isAvailable(long timeoutMillis) {
        return databaseService.isAvailable(timeoutMillis);
    }

    public EvaluationContext createContext(Transaction txn) {
        return new BasicEvaluationContext(txn, databaseService);
    }

}
