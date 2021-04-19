package com.tramchester.graph.caches;

import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.graphbuild.GraphBuilder;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import java.util.Set;

// KEEP for assisting with debugging
public class NodeTypeDirect implements NodeTypeRepository {
    @Override
    public boolean isTime(Node node) {
        return node.hasLabel(GraphBuilder.Labels.MINUTE);
    }

    @Override
    public boolean isHour(Node node) {
        return node.hasLabel(GraphBuilder.Labels.HOUR);
    }

    @Override
    public boolean isBusStation(Node node) {
        return node.hasLabel(GraphBuilder.Labels.BUS_STATION);
    }

    @Override
    public boolean isRouteStation(Node node) {
        return node.hasLabel(GraphBuilder.Labels.ROUTE_STATION);
    }

    @Override
    public boolean isService(Node node) {
        return node.hasLabel(GraphBuilder.Labels.SERVICE);
    }

    @Override
    public boolean isTrainStation(Node node) {
        return node.hasLabel(GraphBuilder.Labels.TRAIN_STATION);
    }

    // for creating query nodes, to support MyLocation journeys
    public Node createQueryNode(GraphDatabase graphDatabase, Transaction txn) {
        return graphDatabase.createNode(txn, GraphBuilder.Labels.QUERY_NODE);
    }

    // for deleting query nodes, to support MyLocation journeys
    public void deleteQueryNode(Node node) {
        node.delete();
    }

}
