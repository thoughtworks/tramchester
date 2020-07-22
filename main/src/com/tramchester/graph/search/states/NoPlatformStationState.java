package com.tramchester.graph.search.states;

import com.tramchester.domain.TransportMode;
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

public class NoPlatformStationState extends TraversalState implements NodeId {

    public static class Builder {

        public NoPlatformStationState from(WalkingState walkingState, Node node, int cost, GraphBuilder.Labels nodeLabel) {
            return new NoPlatformStationState(walkingState, node.getRelationships(OUTGOING, BOARD, INTERCHANGE_BOARD),
                    cost, node.getId(), modeFromLabel(nodeLabel));
        }

        public NoPlatformStationState from(NotStartedState notStartedState, Node node, int cost, GraphBuilder.Labels nodeLabel) {
            return new NoPlatformStationState(notStartedState, getAll(node),
                    cost, node.getId(), modeFromLabel(nodeLabel));
        }

        public TraversalState fromRouteStation(RouteStationStateOnTrip onTrip, Node node, int cost,  GraphBuilder.Labels nodeLabel) {
            // filter so we don't just get straight back on tram if just boarded, or if we are on an existing trip
            List<Relationship> stationRelationships = filterExcludingEndNode(getAll(node), onTrip);
            return new NoPlatformStationState(onTrip, stationRelationships, cost, node.getId(), modeFromLabel(nodeLabel));
        }

        public TraversalState fromRouteStation(RouteStationStateEndTrip routeStationState, Node node, int cost,  GraphBuilder.Labels nodeLabel) {
            // end of a trip, may need to go back to this route station to catch new service
            return new NoPlatformStationState(routeStationState, getAll(node), cost, node.getId(), modeFromLabel(nodeLabel));
        }

        public TraversalState fromNeighbour(NoPlatformStationState noPlatformStation, Node node, int cost, GraphBuilder.Labels nodeLabel) {
            return new NoPlatformStationState(noPlatformStation, node.getRelationships(OUTGOING, BOARD, INTERCHANGE_BOARD),
                    cost, node.getId(), modeFromLabel(nodeLabel));
        }

        public TraversalState fromNeighbour(TramStationState tramStationState, Node node, int cost,  GraphBuilder.Labels nodeLabel) {
            return new NoPlatformStationState(tramStationState, node.getRelationships(OUTGOING, BOARD, INTERCHANGE_BOARD),
                    cost, node.getId(), modeFromLabel(nodeLabel));
        }

        private Iterable<Relationship> getAll(Node node) {
            return node.getRelationships(OUTGOING, INTERCHANGE_BOARD, BOARD, WALKS_FROM,
                    BUS_NEIGHBOUR, TRAM_NEIGHBOUR, TRAIN_NEIGHBOUR);
        }
    }

    private final long stationNodeId;
    private final TransportMode mode;

    private NoPlatformStationState(TraversalState parent, Iterable<Relationship> relationships, int cost, long stationNodeId, TransportMode mode) {
        super(parent, relationships, cost);
        this.stationNodeId = stationNodeId;
        this.mode = mode;
    }

    @Override
    public TraversalState createNextState(Path path, GraphBuilder.Labels nodeLabel, Node next, JourneyState journeyState, int cost) {
        long nodeId = next.getId();
        if (destinationNodeIds.contains(nodeId)) {
            // TODO Cost of bus depart?
            return builders.destination.from(this, cost);
        }

        switch (nodeLabel) {
            case QUERY_NODE:
            case QUERY_NODE_MID:
                return builders.walking.fromNoPlatformStation(this, next, cost);
            case ROUTE_STATION:
                return toRouteStation(next, journeyState, cost);
            case TRAM_STATION:
                return builders.tramStation.fromNeighbour(this, next, cost);
            case BUS_STATION:
            case TRAIN_STATION:
                return builders.noPlatformStation.fromNeighbour(this, next, cost, nodeLabel);
            default:
                throw new RuntimeException("Unexpected node type: " + nodeLabel + " at " + toString());
        }
    }

    @NotNull
    private TraversalState toRouteStation(Node node, JourneyState journeyState, int cost) {
        try {
            journeyState.board(mode);
        } catch (TramchesterException e) {
            throw new RuntimeException("unable to board bus", e);
        }

        return builders.routeStationJustBoarded.fromNoPlatformStation(this, node, cost, mode);
    }

    @Override
    public String toString() {
        return "BusStationState{" +
                "stationNodeId=" + stationNodeId +
                ", mode=" + mode +
                "} " + super.toString();
    }

    @Override
    public long nodeId() {
        return stationNodeId;
    }

}
