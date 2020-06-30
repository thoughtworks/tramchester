package com.tramchester.graph.search.states;

import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.geo.SortsPositions;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.GraphStaticKeys;
import com.tramchester.graph.search.JourneyState;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class BusStationState extends TraversalState implements NodeId {

    public static class Builder {

        public TraversalState fromWalking(WalkingState walkingState, Node node, int cost) {
            return new BusStationState(walkingState, node.getRelationships(OUTGOING, BOARD, INTERCHANGE_BOARD), cost, node.getId());
        }

        public TraversalState fromStart(NotStartedState notStartedState, Node node, int cost) {
            return new BusStationState(notStartedState, node.getRelationships(OUTGOING, INTERCHANGE_BOARD, BOARD, WALKS_FROM), cost,
                    node.getId());
        }

        public TraversalState fromRouteStationOnTrip(RouteStationState routeStationState, Node node, int cost) {
            List<Relationship> stationRelationships = filterExcludingEndNode(
                    node.getRelationships(OUTGOING, BOARD, INTERCHANGE_BOARD, WALKS_FROM), routeStationState);

            // filter so we don't just get straight back on tram if just boarded, or if we are on an existing trip
            return new BusStationState(routeStationState, stationRelationships, cost, node.getId());
        }

        public TraversalState fromRouteStation(RouteStationState routeStationState, Node node, int cost) {
            // TODO Was this a bug?
//        List<Relationship> stationRelationships = filterExcludingEndNode(
//                node.getRelationships(OUTGOING, BOARD, INTERCHANGE_BOARD, WALKS_FROM), routeStationNodeId);

            // end of a trip, may need to go back to this route station to catch new service
            return new BusStationState(routeStationState, node.getRelationships(OUTGOING, BOARD, INTERCHANGE_BOARD, WALKS_FROM),
                    cost, node.getId());
        }
    }

    private final long stationNodeId;
    private final WalkingState.Builder walkingStateBuilder;

    private BusStationState(TraversalState parent, Iterable<Relationship> relationships, int cost, long stationNodeId) {
        super(parent, relationships, cost);
        this.stationNodeId = stationNodeId;
        walkingStateBuilder = new WalkingState.Builder();
    }

    @Override
    public TraversalState createNextState(Path path, GraphBuilder.Labels nodeLabel, Node node, JourneyState journeyState, int cost) {
        long nodeId = node.getId();
        if (nodeId == destinationNodeId) {
            // TODO Cost of bus depart?
            return new DestinationState(this, cost);
        }

        if (nodeLabel == GraphBuilder.Labels.QUERY_NODE) {
            return walkingStateBuilder.fromBusStation(this, node, cost);
        }
        if (nodeLabel == GraphBuilder.Labels.ROUTE_STATION) {
            return toRouteStation(node, journeyState, cost, nodeId);
        }
        throw new RuntimeException("Unexpected node type: "+nodeLabel);

    }

    @NotNull
    private TraversalState toRouteStation(Node node, JourneyState journeyState, int cost, long nodeId) {
        try {
            journeyState.boardBus();
        } catch (TramchesterException e) {
            throw new RuntimeException("unable to board bus", e);
        }
        List<Relationship> outbounds = filterExcludingEndNode(node.getRelationships(OUTGOING,
                DEPART, INTERCHANGE_DEPART), this);

        outbounds.addAll(orderSvcRelationships(node));

        return new RouteStationState(this, outbounds, nodeId, cost, true);
    }

    private Collection<Relationship> orderSvcRelationships(Node node) {
        Iterable<Relationship> toServices = node.getRelationships(OUTGOING, TO_SERVICE);

        List<SortsPositions.HasStationId<Relationship>> relationships = new ArrayList<>();
        toServices.forEach(svcRelationship -> relationships.add(new RelationshipFacade(svcRelationship)));

        return sortsPositions.sortedByNearTo(destinationStationIds, relationships);
    }

    @Override
    public String toString() {
        return "BusStationState{" +
                "stationNodeId=" + stationNodeId +
                ", cost=" + super.getCurrentCost() +
                ", parent=" + parent +
                '}';
    }

    @Override
    public long nodeId() {
        return stationNodeId;
    }

    private static class RelationshipFacade implements SortsPositions.HasStationId<Relationship> {
        private final Relationship relationship;

        private RelationshipFacade(Relationship relationship) {
            this.relationship = relationship;
        }

        @Override
        public String getStationId() {
            return relationship.getProperty(GraphStaticKeys.TOWARDS_STATION_ID).toString();
        }

        @Override
        public Relationship getContained() {
            return relationship;
        }
    }
}
