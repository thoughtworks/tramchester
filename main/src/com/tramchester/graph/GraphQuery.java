package com.tramchester.graph;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.GraphProperty;
import com.tramchester.domain.HasId;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.graphbuild.GraphBuilder;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import javax.inject.Inject;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

@LazySingleton
public class GraphQuery {

    private final GraphDatabase graphDatabase;

    @Inject
    public GraphQuery(GraphDatabase graphDatabase, GraphBuilder.Ready ready) {
        // ready is token to express dependency on having a built graph DB
        this.graphDatabase = graphDatabase;
    }

    public Node getRouteStationNode(Transaction txn, HasId<RouteStation> id) {
        return findNode(txn, GraphBuilder.Labels.ROUTE_STATION, id);
    }

    public Node getStationNode(Transaction txn, Station station) {
        Set<GraphBuilder.Labels> labels = GraphBuilder.Labels.forMode(station.getTransportModes());
        // ought to be able find with any of the labels, so use the first one
        GraphBuilder.Labels label = labels.iterator().next();
        return findNode(txn, label, station);
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
