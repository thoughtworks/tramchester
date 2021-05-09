package com.tramchester.graph.search.states;

import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.graph.search.JourneyState;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.Set;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class NoPlatformStationState extends TraversalState implements NodeId {

    public static class Builder implements TowardsState<NoPlatformStationState> {


        @Override
        public void register(RegistersFromState registers) {
            registers.add(WalkingState.class, this);
            registers.add(NotStartedState.class, this);
            registers.add(RouteStationStateOnTrip.class, this);
            registers.add(RouteStationStateEndTrip.class, this);
            registers.add(NoPlatformStationState.class, this);
            registers.add(TramStationState.class, this);
            registers.add(GroupedStationState.class, this);
        }

        @Override
        public Class<NoPlatformStationState> getDestination() {
            return NoPlatformStationState.class;
        }

        public NoPlatformStationState from(WalkingState walkingState, Node node, int cost) {
            return new NoPlatformStationState(walkingState,
                    node.getRelationships(OUTGOING, BOARD, INTERCHANGE_BOARD, GROUPED_TO_PARENT, NEIGHBOUR),
                    cost, node.getId());
        }

        public NoPlatformStationState from(NotStartedState notStartedState, Node node, int cost) {
            return new NoPlatformStationState(notStartedState, getAll(node), cost, node.getId());
        }

        public TraversalState fromRouteStation(RouteStationStateEndTrip routeStationState, Node node, int cost) {
            // end of a trip, may need to go back to this route station to catch new service
            return new NoPlatformStationState(routeStationState, getAll(node), cost, node.getId());
        }

        public TraversalState fromRouteStation(RouteStationStateOnTrip onTrip, Node node, int cost) {
            // filter so we don't just get straight back on tram if just boarded, or if we are on an existing trip
            Stream<Relationship> stationRelationships = filterExcludingEndNode(getAll(node), onTrip);
            return new NoPlatformStationState(onTrip, stationRelationships, cost, node.getId());
        }

        public TraversalState fromNeighbour(NoPlatformStationState noPlatformStation, Node node, int cost) {
            return new NoPlatformStationState(noPlatformStation,
                    node.getRelationships(OUTGOING, BOARD, INTERCHANGE_BOARD, GROUPED_TO_PARENT),
                    cost, node.getId());
        }

        public TraversalState fromNeighbour(TramStationState tramStationState, Node node, int cost) {
            return new NoPlatformStationState(tramStationState,
                    node.getRelationships(OUTGOING, BOARD, INTERCHANGE_BOARD, GROUPED_TO_PARENT),
                    cost, node.getId());
        }

        public TraversalState fromGrouped(GroupedStationState groupedStationState, Node node, int cost) {
            return new NoPlatformStationState(groupedStationState,
                    node.getRelationships(OUTGOING, BOARD, INTERCHANGE_BOARD, NEIGHBOUR),
                    cost,  node.getId());
        }

        private Iterable<Relationship> getAll(Node node) {
            return node.getRelationships(OUTGOING, INTERCHANGE_BOARD, BOARD, WALKS_FROM, NEIGHBOUR, GROUPED_TO_PARENT);
        }

    }

    private final long stationNodeId;

    private NoPlatformStationState(TraversalState parent, Stream<Relationship> relationships, int cost, long stationNodeId) {
        super(parent, relationships, cost);
        this.stationNodeId = stationNodeId;
    }

    private NoPlatformStationState(TraversalState parent, Iterable<Relationship> relationships, int cost, long stationNodeId) {
        super(parent, relationships, cost);
        this.stationNodeId = stationNodeId;
    }

    // should only be called for multi-mode stations
    @Override
    public TraversalState createNextState(Set<GraphBuilder.Labels> nodeLabels, Node next, JourneyState journeyState, int cost) {
        long nodeId = next.getId();
        if (destinationNodeIds.contains(nodeId)) {
            // TODO Cost of bus depart?
            return builders.destination.from(this, cost);
        }

        // route station nodes may also have INTERCHANGE label set
        if (nodeLabels.contains(GraphBuilder.Labels.ROUTE_STATION)) {
            return toRouteStation(next, journeyState, cost);
        }

        // TODO this is not called any more
        // return builders.noPlatformStation.fromNeighbour(this, next, cost);

        throw new UnexpectedNodeTypeException(next, "Unexpected node type: " + nodeLabels + " at " + this);
    }

    @Override
    public TraversalState createNextState(GraphBuilder.Labels nodeLabel, Node next, JourneyState journeyState, int cost) {
        long nodeId = next.getId();
        if (destinationNodeIds.contains(nodeId)) {
            // TODO Cost of bus depart?
            return builders.destination.from(this, cost);
        }

        switch (nodeLabel) {
            case QUERY_NODE:
                journeyState.walkingConnection();
                return builders.walking.fromNoPlatformStation(this, next, cost);
            case ROUTE_STATION:
                return toRouteStation(next, journeyState, cost);
            case TRAM_STATION:
                return builders.tramStation.fromNeighbour(this, next, cost);
            case BUS_STATION:
            case TRAIN_STATION:
                return builders.towardsNeighbour(this, NoPlatformStationState.class).fromNeighbour(this, next, cost);
            case GROUPED:
                return builders.groupedStation.fromChildStation(this, next, cost); // grouped are same transport mode
            default:
                throw new UnexpectedNodeTypeException(next, "Unexpected node type: " + nodeLabel + " at " + this);
        }
    }

    @NotNull
    private TraversalState toRouteStation(Node node, JourneyState journeyState, int cost) {
        TransportMode actualMode = GraphProps.getTransportMode(node);

        try {
            journeyState.board(actualMode);
        } catch (TramchesterException e) {
            throw new RuntimeException("unable to board vehicle", e);
        }

        return builders.towardsRouteStationJustBoarded(this, RouteStationStateJustBoarded.class).
                fromNoPlatformStation(this, node, cost, actualMode);
    }

    @Override
    public String toString() {
        return "NoPlatformStationState{" +
                "stationNodeId=" + stationNodeId +
                "} " + super.toString();
    }

    @Override
    public long nodeId() {
        return stationNodeId;
    }

}
