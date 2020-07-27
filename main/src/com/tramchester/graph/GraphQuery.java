package com.tramchester.graph;

import com.tramchester.domain.*;
import com.tramchester.domain.places.RouteStation;
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

    public Node getPlatformNode(Transaction txn, IdFor<Platform> id) {
        return getNodeByLabelAndID(txn, id, GraphBuilder.Labels.PLATFORM);
    }

    public Node getRouteStationNode(Transaction txn, IdFor<RouteStation> id) {
        return getNodeByLabelAndID(txn, id, GraphBuilder.Labels.ROUTE_STATION);
    }

    public Node getStationNode(Transaction txn, Station station) {
        return getStationNode(txn, station.getId(), station.getTransportMode());
    }

    @Deprecated
    private <T extends HasId<T> & GraphProperty> Node getNodeByLabelAndID(Transaction txn, IdFor<T> id, GraphBuilder.Labels label) {
        return graphDatabase.findNode(txn, label, GraphPropertyKey.ID.getText(), id.getGraphId());
    }

    public List<Relationship> getRouteStationRelationships(Transaction txn, IdFor<RouteStation> routeStationId, Direction direction) {
        Node routeStationNode = getRouteStationNode(txn, routeStationId);
        if (routeStationNode==null) {
            return Collections.emptyList();
        }
        List<Relationship> result = new LinkedList<>();
        routeStationNode.getRelationships(direction, TransportRelationshipTypes.forPlanning()).forEach(result::add);
        return result;
    }

    private Node getStationNode(Transaction txn, IdFor<Station> stationId, TransportMode transportMode) {
        Node node = getNodeByLabelAndID(txn, stationId, GraphBuilder.Labels.forMode(transportMode));
        if (node==null) {
            logger.warn("Did not find node for station: " + stationId + " mode: " + transportMode);
        }
        return node;
    }

    public boolean hasNodeForStation(Transaction txn, Station station) {
        return getNodeByLabelAndID(txn, station.getId(), GraphBuilder.Labels.forMode(station.getTransportMode()))!=null;
    }
}
