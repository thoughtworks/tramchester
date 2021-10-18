package com.tramchester.graph;

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

    @Inject
    public GraphDatabaseStoredVersions(TramchesterConfig config) {
        this.config = config;
    }

    public boolean upToDate(GraphDatabaseService databaseService, Set<DataSourceInfo> dataSourceInfo) {
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

        boolean fromDB = !nodes.isEmpty(); // presence of node means neighbours present
        boolean fromConfig = config.getCreateNeighbours();

        if (fromDB==fromConfig) {
            logger.info("CreateNeighbours config matches DB setting of: " + fromDB);
        } else {
            logger.warn("CreateNeighbours config does not match DB setting of: " + fromDB);
        }
        return fromConfig != fromDB;
    }

    private List<Node> getNodes(Transaction transaction, GraphLabel label) {
        ResourceIterator<Node> query = transaction.findNodes(label); // findNodes(transaction, label);
        List<Node> nodes = query.stream().collect(Collectors.toList());

        if (nodes.size()>1) {
            logger.warn("Too many "+label.name()+ " nodes, will use first");
        }
        return nodes;
    }
}
