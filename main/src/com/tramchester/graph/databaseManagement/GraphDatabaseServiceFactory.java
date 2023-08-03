package com.tramchester.graph.databaseManagement;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.GraphDBConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.metrics.Timing;
import org.neo4j.configuration.BootloaderSettings;
import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.configuration.SettingValueParsers;
import org.neo4j.configuration.connectors.BoltConnector;
import org.neo4j.configuration.connectors.HttpConnector;
import org.neo4j.configuration.connectors.HttpsConnector;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.event.DatabaseEventContext;
import org.neo4j.graphdb.event.DatabaseEventListener;
import org.neo4j.io.ByteUnit;
import org.neo4j.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.nio.file.Path;

import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;

@LazySingleton
public class GraphDatabaseServiceFactory implements DatabaseEventListener {
    private static final Logger logger = LoggerFactory.getLogger(GraphDatabaseServiceFactory.class);

    private static final int STARTUP_TIMEOUT = 1000;
    private final GraphDBConfig dbConfig;
    private final String dbName;
    private final Path graphFile;
    private DatabaseManagementService managementServiceImpl;

    @Inject
    public GraphDatabaseServiceFactory(TramchesterConfig config) {
        dbConfig = config.getGraphDBConfig();
        dbName = DEFAULT_DATABASE_NAME; // must be this for neo4j community edition default DB
        graphFile = dbConfig.getDbPath().toAbsolutePath();
    }

    @PostConstruct
    private void start() {
        logger.info("start");
        logger.info("DBName : '"+ dbName + "' Path:'" + graphFile.toString() +"'");
        //createManagementService(); - slow, only do when needed
        logger.info("started");
    }

    @PreDestroy
    private void stop() {
        logger.info("Stopping");
        if (managementServiceImpl!=null) {
            logger.warn("DatabaseManagementService was not previously shutdown");
            managementServiceImpl.unregisterDatabaseEventListener(this);
            managementServiceImpl.shutdown();
        } else {
            logger.info("DatabaseManagementService was previously shutdown");
        }
        logger.info("stopped");
    }

    private void createManagementService() {

        try (Timing ignored = new Timing(logger, "DatabaseManagementService build")) {
            Long neo4jPagecacheMemory = ByteUnit.parse(dbConfig.getNeo4jPagecacheMemory());
            managementServiceImpl = new DatabaseManagementServiceBuilder( graphFile ).
//                    setConfig(GraphDatabaseSettings.track_query_allocation, false).
//                    setConfig(GraphDatabaseSettings.store_internal_log_level, Level.WARN ).

                    // see https://neo4j.com/docs/operations-manual/current/performance/memory-configuration/#heap-sizing
                    setConfig(GraphDatabaseSettings.pagecache_memory, neo4jPagecacheMemory).

                    // TODO This one into config?
                    //setConfig(GraphDatabaseSettings.tx_state_max_off_heap_memory, SettingValueParsers.BYTES.parse("256m")).

                    // NOTE: dbms.memory.transaction.total.max is 70% of heap size limit
                    setConfig(BootloaderSettings.max_heap_size, SettingValueParsers.BYTES.parse("512m")).

                    // deprecated
                    //setConfig(GraphDatabaseSettings.tx_state_max_off_heap_memory, SettingValueParsers.BYTES.parse("512m")).

                    // txn logs, no need to save beyond current one
                    setConfig(GraphDatabaseSettings.keep_logical_logs, "false").

                    // operating in embedded mode
                    setConfig(HttpConnector.enabled, false).
                    setConfig(HttpsConnector.enabled, false).
                    setConfig(BoltConnector.enabled, false).

                    // TODO no 4.2 version available?
                    //setUserLogProvider(new Slf4jLogProvider()).
                    build();
        }

        managementServiceImpl.registerDatabaseEventListener(this);
    }

    private DatabaseManagementService getManagementService() {
        if (managementServiceImpl==null) {
            logger.info("Starting DatabaseManagementService");
            createManagementService();
        } else {
            logger.info("DatabaseManagementService was already running");
        }
        return managementServiceImpl;
    }

    public void shutdownDatabase() {
        logger.info("Shutdown");
        // NOTE: cannot shutdown using name in community edition
        // managementService.shutdownDatabase(dbName);
        // ALSO: have to recreate managementService after shutdown otherwise DB does not start
        if (managementServiceImpl==null) {
            logger.error("Attempt to shutdown when DatabaseManagementService already stopped");
        } else {
            logger.info("Stopping DatabaseManagementService");
            managementServiceImpl.unregisterDatabaseEventListener(this);
            managementServiceImpl.shutdown();
            managementServiceImpl = null;
        }

        logger.info("Stopped");
    }

    public GraphDatabaseService create() {
        // for community edition name must be DEFAULT_DATABASE_NAME
        logger.info("Start for " + dbName + " at " + graphFile.toString());

        // create DB service for our DB
        DatabaseManagementService managementService = getManagementService();
        GraphDatabaseService graphDatabaseService = managementService.database(dbName);

        managementService.listDatabases().forEach(databaseName -> {
            logger.info("Database from managementService: " + databaseName);
        });

        logger.info("Wait for GraphDatabaseService available");
        int retries = 100;
        // NOTE: DB can just silently fail to start if updated net4j versions, so cleanGraph in this scenario
        while (!graphDatabaseService.isAvailable(STARTUP_TIMEOUT) && retries>0) {
            logger.error("GraphDatabaseService is not available (neo4j updated? dbClean needed?), name: " + dbName +
                    " Path: " + graphFile.toAbsolutePath() + " check " + retries);
            retries--;
        }

        if (!graphDatabaseService.isAvailable(STARTUP_TIMEOUT)) {
            throw new RuntimeException("Could not start " + dbConfig.getDbPath());
        }

        logger.info("GraphDatabaseService is available");
        return graphDatabaseService;
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

    @Override
    public void databaseCreate(DatabaseEventContext eventContext) {
        logger.info("database event: create " + eventContext.getDatabaseName());
    }

    @Override
    public void databaseDrop(DatabaseEventContext eventContext) {
        logger.info("database event: drop " + eventContext.getDatabaseName());
    }

}
