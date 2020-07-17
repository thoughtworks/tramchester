package com.tramchester.graph;

import com.tramchester.domain.TransportMode;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.graphbuild.GraphBuilder;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class GraphQuery {

    // TODO REFACTOR Methods only used for tests into own class
    private static final Logger logger = LoggerFactory.getLogger(GraphQuery.class);

    private final GraphDatabase graphDatabase;

    public GraphQuery(GraphDatabase graphDatabase) {
        this.graphDatabase = graphDatabase;
    }

    public Node getPlatformNode(Transaction txn, String id) {
        return getNodeByLabel(txn, id, GraphBuilder.Labels.PLATFORM);
    }

    public Node getRouteStationNode(Transaction txn, String id) {
        return getNodeByLabel(txn, id,GraphBuilder.Labels.ROUTE_STATION);
    }

    private Node getNodeByLabel(Transaction txn, String id, GraphBuilder.Labels label) {
        Node node = graphDatabase.findNode(txn, label, GraphStaticKeys.ID, id);
//        if (node==null) {
//            logger.warn("Failed to find node for label " + label + " and id '"+id+"'");
//        }
        return node;
    }

    public List<Relationship> getRouteStationRelationships(Transaction txn, String routeStationId, Direction direction) {
        Node routeStationNode = getRouteStationNode(txn, routeStationId);
        if (routeStationNode==null) {
            return Collections.emptyList();
        }
        List<Relationship> result = new LinkedList<>();
        routeStationNode.getRelationships(direction, TransportRelationshipTypes.forPlanning()).forEach(result::add);
        return result;
    }

    public Node getStationNode(Transaction txn, Station station) {
        return getStationNode(txn, station.getId(), station.getTransportMode());
    }

    private Node getStationNode(Transaction txn, String stationId, TransportMode transportMode) {
        return getNodeByLabel(txn, stationId, GraphBuilder.Labels.forMode(transportMode));
    }

}
