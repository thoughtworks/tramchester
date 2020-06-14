package com.tramchester.graph;

import com.tramchester.domain.places.Station;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class GraphQuery {

    // TODO REFACTOR Methods only used for tests into own class
    // private static final Logger logger = LoggerFactory.getLogger(GraphQuery.class);

    private final GraphDatabase graphDatabase;

    public GraphQuery(GraphDatabase graphDatabase) {
        this.graphDatabase = graphDatabase;
    }

    public Node getTramStationNode(Transaction txn, String stationId) {
        return getNodeByLabel(txn, stationId, GraphBuilder.Labels.TRAM_STATION);
    }

    public Node getBusStationNode(Transaction txn, String stationId) {
        return getNodeByLabel(txn, stationId, GraphBuilder.Labels.BUS_STATION);
    }

    public Node getPlatformNode(Transaction txn, String id) {
        return getNodeByLabel(txn, id, GraphBuilder.Labels.PLATFORM);
    }

    public Node getServiceNode(Transaction txn, String id) {
        GraphBuilder.Labels Label = GraphBuilder.Labels.SERVICE;
        return getNodeByLabel(txn, id, Label);
    }

    public Node getRouteStationNode(Transaction txn, String id) {
        return getNodeByLabel(txn, id,GraphBuilder.Labels.ROUTE_STATION);
    }

    public Node getHourNode(Transaction txn, String id) {
        return getNodeByLabel(txn, id, GraphBuilder.Labels.HOUR);
    }

    private Node getNodeByLabel(Transaction txn, String id, GraphBuilder.Labels label) {
        return graphDatabase.findNode(txn, label, GraphStaticKeys.ID, id);
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
        String stationId = station.getId();
        if (station.isTram()) {
            return getTramStationNode(txn, stationId);
        } else {
            return getBusStationNode(txn, stationId);
        }
    }
}
