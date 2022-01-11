package com.tramchester.graph;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.GraphProperty;
import com.tramchester.domain.Platform;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.graphbuild.GraphLabel;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import javax.inject.Inject;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/***
 * Make sure have correct dependencies on "Ready" tokens alongside this class, it makes no guarantees for any
 * data having put in the DB.
 * It can't have a ready token injected as this would create circular dependencies.
 */
@LazySingleton
public class GraphQuery {

    private final GraphDatabase graphDatabase;

    @Inject
    public GraphQuery(GraphDatabase graphDatabase) {
        this.graphDatabase = graphDatabase;
    }

    /**
     * When calling from tests make sure relevant DB is fully built
     */
    public Node getRouteStationNode(Transaction txn, HasId<RouteStation> id) {
        return findNode(txn, GraphLabel.ROUTE_STATION, id);
    }

    /**
     * When calling from tests make sure relevant DB is fully built
     */
    public Node getStationNode(Transaction txn, Station station) {
        Set<GraphLabel> labels = GraphLabel.forMode(station.getTransportModes());
        // ought to be able find with any of the labels, so use the first one
        GraphLabel label = labels.iterator().next();
        return findNode(txn, label, station);
    }

    private Node getGroupedNode(Transaction txn, Station station) {
        return findNode(txn, GraphLabel.GROUPED, station);
    }

    /**
     * When calling from tests make sure relevant DB is fully built
     */
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

    /**
     * When calling from tests make sure relevant DB is fully built
     */
    public List<Relationship> getRouteStationRelationships(Transaction txn, HasId<RouteStation> routeStation, Direction direction) {
        Node routeStationNode = getRouteStationNode(txn, routeStation);
        if (routeStationNode==null) {
            return Collections.emptyList();
        }
        List<Relationship> result = new LinkedList<>();
        routeStationNode.getRelationships(direction, TransportRelationshipTypes.forPlanning()).forEach(result::add);
        return result;
    }

    public boolean hasAnyNodesWithLabelAndId(Transaction txn, GraphLabel label, String property, String key) {
        Node node = graphDatabase.findNode(txn, label, property, key);
        return node != null;
    }

    public Node getPlatformNode(Transaction txn, Platform platform) {
        return findNode(txn, GraphLabel.PLATFORM, platform);
    }
}
