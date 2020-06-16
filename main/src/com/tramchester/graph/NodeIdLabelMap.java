package com.tramchester.graph;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.picocontainer.Disposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.tramchester.graph.GraphBuilder.Labels.BUS_STATION;
import static com.tramchester.graph.GraphBuilder.Labels.ROUTE_STATION;

public class NodeIdLabelMap implements Disposable, NodeTypeRepository {
    private static final Logger logger = LoggerFactory.getLogger(NodeIdLabelMap.class);

    // map from the NodeId to the Label
    private final Map<GraphBuilder.Labels, Set<Long>> map;
    private final ConcurrentMap<Long, Boolean> queryNodes;

    public NodeIdLabelMap() {
        map = new EnumMap<>(GraphBuilder.Labels.class);
        for (GraphBuilder.Labels label: GraphBuilder.Labels.values()) {
            if (label != GraphBuilder.Labels.QUERY_NODE) {
                map.put(label, new HashSet<>(getCapacity(label), 1.0F));
            }
        }
        queryNodes = new ConcurrentHashMap<>();
    }

    // called when loaded from disc, instead of rebuild
    public void populateNodeLabelMap(GraphDatabase graphDatabase) {
        logger.info("Rebuilding node->label index");
        GraphBuilder.Labels[] labels = GraphBuilder.Labels.values();
        try (Transaction tx = graphDatabase.beginTx()) {
            for (GraphBuilder.Labels label : labels) {
                graphDatabase.findNodes(tx, label).stream().forEach(node -> put(node.getId(), label));
            }
        }
        for (GraphBuilder.Labels label : labels) {
            if (label != GraphBuilder.Labels.QUERY_NODE) {
                logger.info("Loaded " + map.get(label).size() + " for label " + label);
            }
        }
        logger.info("Finished populating map");
    }

    @Override
    public void dispose() {
        queryNodes.clear();
        map.clear();
    }

    private int getCapacity(GraphBuilder.Labels label) {
        // approx. sizings
        switch (label) {
            case ROUTE_STATION: return 282;
            case TRAM_STATION: return 93;
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

    public boolean isHour(Node node) {
        return has(GraphBuilder.Labels.HOUR, node.getId());
    }

    public boolean isTime(Node node) { return has(GraphBuilder.Labels.MINUTE, node.getId()); }

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
