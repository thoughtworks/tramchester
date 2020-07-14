package com.tramchester.graph.search.states;

import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.search.JourneyState;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

import java.util.List;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class BusStationState extends TraversalState implements NodeId {

    public static class Builder {

        public BusStationState from(WalkingState walkingState, Node node, int cost) {
            return new BusStationState(walkingState, node.getRelationships(OUTGOING, BOARD, INTERCHANGE_BOARD),
                    cost, node.getId());
        }

        public BusStationState from(NotStartedState notStartedState, Node node, int cost) {
            return new BusStationState(notStartedState, getAll(node),
                    cost, node.getId());
        }

        public TraversalState fromRouteStation(RouteStationStateOnTrip onTrip, Node node, int cost) {
            // filter so we don't just get straight back on tram if just boarded, or if we are on an existing trip
            List<Relationship> stationRelationships = filterExcludingEndNode(getAll(node), onTrip);
            return new BusStationState(onTrip, stationRelationships, cost, node.getId());
        }

        public TraversalState fromRouteStation(RouteStationStateEndTrip routeStationState, Node node, int cost) {
            // end of a trip, may need to go back to this route station to catch new service
            return new BusStationState(routeStationState, getAll(node), cost, node.getId());
        }

        public TraversalState fromNeighbour(BusStationState busStationState, Node node, int cost) {
            return new BusStationState(busStationState, node.getRelationships(OUTGOING, BOARD, INTERCHANGE_BOARD),
                    cost, node.getId());
        }

        public TraversalState fromNeighbour(TramStationState tramStationState, Node node, int cost) {
            return new BusStationState(tramStationState, node.getRelationships(OUTGOING, BOARD, INTERCHANGE_BOARD),
                    cost, node.getId());
        }

        private Iterable<Relationship> getAll(Node node) {
            return node.getRelationships(OUTGOING, INTERCHANGE_BOARD, BOARD, WALKS_FROM, BUS_NEIGHBOUR, TRAM_NEIGHBOUR);
        }
    }

    private final long stationNodeId;

    private BusStationState(TraversalState parent, Iterable<Relationship> relationships, int cost, long stationNodeId) {
        super(parent, relationships, cost);
        this.stationNodeId = stationNodeId;
    }

    @Override
    public TraversalState createNextState(Path path, GraphBuilder.Labels nodeLabel, Node next, JourneyState journeyState, int cost) {
        long nodeId = next.getId();
        if (destinationNodeIds.contains(nodeId)) {
            // TODO Cost of bus depart?
            return builders.destination.from(this, cost);
        }

        if (nodeLabel == GraphBuilder.Labels.QUERY_NODE || nodeLabel == GraphBuilder.Labels.QUERY_NODE_MID) {
            return builders.walking.fromBusStation(this, next, cost);
        }

        if (nodeLabel == GraphBuilder.Labels.ROUTE_STATION) {
            return toRouteStation(next, journeyState, cost);
        }

        if (nodeLabel == GraphBuilder.Labels.TRAM_STATION) {
            return builders.tramStation.fromNeighbour(this, next, cost);
        }

        if (nodeLabel == GraphBuilder.Labels.BUS_STATION) {
            return builders.busStation.fromNeighbour(this, next, cost);
        }

        throw new RuntimeException("Unexpected node type: " + nodeLabel + " at " + toString());

    }

    @NotNull
    private TraversalState toRouteStation(Node node, JourneyState journeyState, int cost) {
        try {
            journeyState.boardBus();
        } catch (TramchesterException e) {
            throw new RuntimeException("unable to board bus", e);
        }

        return builders.routeStationJustBoarded.fromBusStation(this, node, cost);
    }

    @Override
    public String toString() {
        return "BusStationState{" +
                "stationNodeId=" + stationNodeId +
                "} " + super.toString();
    }

    @Override
    public long nodeId() {
        return stationNodeId;
    }

}
