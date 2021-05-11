package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.graph.search.JourneyState;
import com.tramchester.graph.search.stateMachine.RegistersFromState;
import com.tramchester.graph.search.stateMachine.Towards;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.GROUPED_TO_CHILD;

public class GroupedStationState extends TraversalState {

    public static class Builder implements Towards<GroupedStationState> {

        // TODO map of accept states to outbound relationships

        @Override
        public void register(RegistersFromState registers) {
            registers.add(TramStationState.class, this);
            registers.add(NoPlatformStationState.class, this);
            registers.add(WalkingState.class, this);
            registers.add(NotStartedState.class, this);
        }

        @Override
        public Class<GroupedStationState> getDestination() {
            return GroupedStationState.class;
        }

        public TraversalState fromChildStation(StationState stationState, Node node, int cost) {
            return new GroupedStationState(stationState,
                    filterExcludingEndNode(node.getRelationships(Direction.OUTGOING, GROUPED_TO_CHILD),stationState),
                    cost, node.getId());
        }

        public TraversalState fromWalk(WalkingState walkingState, Node node, int cost) {
            return new GroupedStationState(walkingState, node.getRelationships(Direction.OUTGOING, GROUPED_TO_CHILD),
                    cost, node.getId());
        }

        public TraversalState fromStart(NotStartedState notStartedState, Node node, int cost) {
            return new GroupedStationState(notStartedState, node.getRelationships(Direction.OUTGOING, GROUPED_TO_CHILD),
                    cost, node.getId());
        }
    }

    private final long stationNodeId;

    private GroupedStationState(TraversalState parent, Stream<Relationship> relationships, int cost, long stationNodeId) {
        super(parent, relationships, cost);
        this.stationNodeId = stationNodeId;
    }

    private GroupedStationState(TraversalState parent, Iterable<Relationship> relationships, int cost, long stationNodeId) {
        super(parent, relationships, cost);
        this.stationNodeId = stationNodeId;
    }

    @Override
    public String toString() {
        return "GroupedStationState{" +
                "stationNodeId=" + stationNodeId +
                "} " + super.toString();
    }

    @Override
    protected TramStationState toTramStation(TramStationState.Builder towardsStation, Node node, int cost, JourneyState journeyState) {
        return towardsStation.fromGrouped(this, node, cost);
    }

    @Override
    protected TraversalState toNoPlatformStation(NoPlatformStationState.Builder towardsStation, Node node, int cost, JourneyState journeyState) {
        return towardsStation.fromGrouped(this, node, cost);
    }
}
