package com.tramchester.graph;

import com.tramchester.graph.graphbuild.GraphBuilder;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.picocontainer.Disposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.tramchester.graph.graphbuild.GraphBuilder.Labels.*;

@Singleton
public class NodeIdLabelMap implements Disposable, NodeTypeRepository {
    private static final Logger logger = LoggerFactory.getLogger(NodeIdLabelMap.class);

    // map from the NodeId to the Label
    private final Map<GraphBuilder.Labels, Set<Long>> map;
    private final ConcurrentMap<Long, Boolean> queryNodes;

    private final Set<GraphBuilder.Labels> nodesToCache = new HashSet<>(Arrays.asList(
            ROUTE_STATION, PLATFORM, SERVICE, HOUR, MINUTE, TRAM_STATION, BUS_STATION, TRAIN_STATION
    ));

    public NodeIdLabelMap() {
        map = new EnumMap<>(GraphBuilder.Labels.class);

        for (GraphBuilder.Labels label: nodesToCache) {
            map.put(label, new HashSet<>(getCapacity(label), 1.0F));
        }
        queryNodes = new ConcurrentHashMap<>();
    }

    // called when DB loaded from disc, instead of rebuild
    public void populateNodeLabelMap(GraphDatabase graphDatabase) {
        logger.info("Rebuilding node->label index");
        try (Transaction tx = graphDatabase.beginTx()) {
            for (GraphBuilder.Labels label : nodesToCache) {
                graphDatabase.findNodes(tx, label).stream().forEach(node -> put(node.getId(), label));
            }
        }
        for (GraphBuilder.Labels label : nodesToCache) {
            int size = map.get(label).size();
            if (size>0) {
                logger.info("Loaded " + size + " for label " + label);
            } else {
                logger.info("Loaded zero nodes for label " + label);
            }
        }

        logger.info("Finished populating map");
    }

    @Override
    public void dispose() {
        logger.info("dispose");
        queryNodes.clear();
        map.clear();
    }

    private int getCapacity(GraphBuilder.Labels label) {
        // approx. sizings
        switch (label) {
            case ROUTE_STATION: return 282;
            case TRAM_STATION: return 93;
            case TRAIN_STATION: return 3000;
            case PLATFORM: return 185;
            case SERVICE: return 8680;
            case HOUR: return 62525;
            case MINUTE: return 314150;
            default: return 0;
        }
    }

    public void put(long id, GraphBuilder.Labels label) {
        map.get(label).add(id);
    }

    public boolean isService(Node nodeId) {
        return has(GraphBuilder.Labels.SERVICE, nodeId.getId());
    }

    @Override
    public boolean isTrainStation(Node node) {
        return has(TRAIN_STATION, node.getId());
    }

    public boolean isHour(Node node) {
        return has(HOUR, node.getId());
    }

    public boolean isTime(Node node) { return has(MINUTE, node.getId()); }

    public boolean isRouteStation(Node node) {
        return has(ROUTE_STATION, node.getId());
    }

    public boolean isBusStation(Node node) { return has(BUS_STATION, node.getId()); }

    private boolean has(final GraphBuilder.Labels label, final long nodeId) {
        if (label == GraphBuilder.Labels.QUERY_NODE) {
            return queryNodes.containsKey(nodeId);
        }
        return map.get(label).contains(nodeId);
    }

    // for creating query nodes, to support MyLocation journeys
    public Node createQueryNode(GraphDatabase graphDatabase, Transaction txn) {
        Node result = graphDatabase.createNode(txn, GraphBuilder.Labels.QUERY_NODE);
        queryNodes.put(result.getId(),true);
        return result;
    }

    // for deleting query nodes, to support MyLocation journeys
    public void deleteQueryNode(Node node) {
        long id = node.getId();
        node.delete();
        queryNodes.remove(id);
    }

}
