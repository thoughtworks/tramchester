package com.tramchester.graph;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.GraphProperty;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.graphbuild.GraphLabel;
import org.neo4j.graphdb.*;

import javax.inject.Inject;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/***
 * Make sure have correct dependencies on "Ready" tokens alongside this class, it makes no gaurantees for any
 * data having put in the DB
 */
@LazySingleton
public class GraphQuery {

    private final GraphDatabase graphDatabase;

    @Inject
    public GraphQuery(GraphDatabase graphDatabase) {
        //, StagedTransportGraphBuilder.Ready dbReady, CompositeStationGraphBuilder.Ready compositeStationsReady)
        // ready is token to express dependency on having a built graph DB
        this.graphDatabase = graphDatabase;
    }

    public Node getRouteStationNode(Transaction txn, HasId<RouteStation> id) {
        return findNode(txn, GraphLabel.ROUTE_STATION, id);
    }

    public Node getStationNode(Transaction txn, Station station) {
        Set<GraphLabel> labels = GraphLabel.forMode(station.getTransportModes());
        // ought to be able find with any of the labels, so use the first one
        GraphLabel label = labels.iterator().next();
        return findNode(txn, label, station);
    }

    private Node getGroupedNode(Transaction txn, Station station) {
        return findNode(txn, GraphLabel.GROUPED, station);
    }

    public Node getStationOrGrouped(Transaction txn, Station station) {
        if (station.isComposite()) {
            return getGroupedNode(txn, station);
        } else {
            return getStationNode(txn, station);
        }
    }

    private <C extends GraphProperty>  Node findNode(Transaction txn, GraphLabel label, HasId<C> hasId) {
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

    public boolean hasAnyNodesWithLabel(Transaction txn, GraphLabel label) {
        ResourceIterator<Node> query = graphDatabase.findNodes(txn, label);
        List<Node> nodes = query.stream().collect(Collectors.toList());
        return !nodes.isEmpty();
    }

}
