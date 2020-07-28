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

    private static final Logger logger = LoggerFactory.getLogger(GraphQuery.class);

    private final GraphDatabase graphDatabase;

    public GraphQuery(GraphDatabase graphDatabase) {
        this.graphDatabase = graphDatabase;
    }

    public Node getPlatformNode(Transaction txn, HasId<Platform> id) {
        return findNode(txn, GraphBuilder.Labels.PLATFORM, id);
    }

    public Node getRouteStationNode(Transaction txn, HasId<RouteStation> id) {
        return findNode(txn, GraphBuilder.Labels.ROUTE_STATION, id);
    }

    public Node getStationNode(Transaction txn, Station station) {
        return findNode(txn, GraphBuilder.Labels.forMode(station.getTransportMode()), station);
    }

    public boolean hasNodeForStation(Transaction txn, Station station) {
        return getStationNode(txn, station)!=null;
    }

    private <C extends GraphProperty>  Node findNode(Transaction txn, GraphBuilder.Labels label, HasId<C> hasId) {
        return graphDatabase.findNode(txn, label, hasId.getProp().getText(), hasId.getId().getGraphId());
    }

    public List<Relationship> getRouteStationRelationships(Transaction txn, HasId<RouteStation> routeStation, Direction direction) {
        Node routeStationNode = getRouteStationNode(txn, routeStation);
        if (routeStationNode==null) {
            return Collections.emptyList();
        }
        List<Relationship> result = new LinkedList<>();
        routeStationNode.getRelationships(direction, TransportRelationshipTypes.forPlanning()).forEach(result::add);
        return result;
    }
}
