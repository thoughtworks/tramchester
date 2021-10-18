package com.tramchester.graph.databaseManagement;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.DataSourceInfo;
import com.tramchester.graph.graphbuild.GraphLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;

@LazySingleton
public class GraphDatabaseStoredVersions {
    private static final Logger logger = LoggerFactory.getLogger(GraphDatabaseStoredVersions.class);

    private final TramchesterConfig config;
    private final GraphDatabaseMetaInfo databaseMetaInfo;

    @Inject
    public GraphDatabaseStoredVersions(TramchesterConfig config, GraphDatabaseMetaInfo databaseMetaInfo) {
        this.config = config;
        this.databaseMetaInfo = databaseMetaInfo;
    }

    public boolean upToDate(GraphDatabaseService databaseService, Set<DataSourceInfo> dataSourceInfo) {
        logger.info("Checking graph version information ");

        // version -> flag
        Map<DataSourceInfo, Boolean> upToDate = new HashMap<>();
        try(Transaction transaction = databaseService.beginTx()) {

            if (neighboursEnabledMismatch(transaction)) {
                return false;
            }

            if (!databaseMetaInfo.hasVersionInfo(transaction)) {
                logger.warn("Missing VERSION node, cannot check versions");
                return false;
            }

            Map<String, String> versionsFromDB = databaseMetaInfo.getVersions(transaction);

            if (versionsFromDB.size()!=dataSourceInfo.size()) {
                logger.warn("VERSION node property mismatch, got " +versionsFromDB.size() + " expected " + dataSourceInfo.size());
                return false;
            }

            dataSourceInfo.forEach(sourceInfo -> {
                DataSourceID sourceName = sourceInfo.getID();
                String name = sourceName.name();
                logger.info("Checking version for " + sourceName);

                if (versionsFromDB.containsKey(name)) {
                    String graphValue = versionsFromDB.get(name);
                    boolean matches = sourceInfo.getVersion().equals(graphValue);
                    upToDate.put(sourceInfo, matches);
                    if (matches) {
                        logger.info("Got correct VERSION node value for " + sourceInfo);
                    } else {
                        logger.warn(format("Mismatch on graph VERSION, got '%s' for %s", graphValue, sourceInfo));
                    }
                } else {
                    upToDate.put(sourceInfo, false);
                    logger.warn("Could not find version for " + name + " properties were " + versionsFromDB);
                }
            });
        }
        return upToDate.values().stream().allMatch(flag -> flag);
    }

    private boolean neighboursEnabledMismatch(Transaction txn) {

        boolean fromDB = databaseMetaInfo.isNeighboursEnabled(txn);
        boolean fromConfig = config.getCreateNeighbours();

        if (fromDB==fromConfig) {
            logger.info("CreateNeighbours config matches DB setting of: " + fromDB);
        } else {
            logger.warn("CreateNeighbours config does not match DB setting of: " + fromDB);
        }
        return fromConfig != fromDB;
    }

}
