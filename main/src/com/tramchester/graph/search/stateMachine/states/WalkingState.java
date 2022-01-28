package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.RegistersFromState;
import com.tramchester.graph.search.stateMachine.Towards;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.List;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.WALKS_TO_STATION;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class WalkingState extends TraversalState {

    public static class Builder implements Towards<WalkingState> {

        @Override
        public void register(RegistersFromState registers) {
            registers.add(NotStartedState.class, this);
            registers.add(NoPlatformStationState.class, this);
            registers.add(PlatformStationState.class, this);

            // todo needed?
            //registers.add(GroupedStationState.class, this);
        }

        @Override
        public Class<WalkingState> getDestination() {
            return WalkingState.class;
        }

        public TraversalState fromStart(NotStartedState notStartedState, Node firstNode, int cost) {
            final Iterable<Relationship> relationships = firstNode.getRelationships(OUTGOING, WALKS_TO_STATION);
            List<Relationship> towardsDest = notStartedState.traversalOps.getTowardsDestination(relationships);

            // prioritise a direct walk from start if one is available
            if (towardsDest.isEmpty()) {
                return new WalkingState(notStartedState, relationships, cost);
            } else {
                return new WalkingState(notStartedState, towardsDest.stream(), cost);
            }
        }

        public TraversalState fromStation(StationState station, Node node, int cost) {
            return new WalkingState(station,
                    filterExcludingEndNode(node.getRelationships(OUTGOING), station), cost);
        }

    }

    private WalkingState(TraversalState parent, Stream<Relationship> relationships, int cost) {
        super(parent, relationships, cost);
    }

    private WalkingState(TraversalState parent, Iterable<Relationship> relationships, int cost) {
        super(parent, relationships, cost);
    }

    @Override
    public String toString() {
        return "WalkingState{} " + super.toString();
    }

    @Override
    protected PlatformStationState toTramStation(PlatformStationState.Builder towardsStation, Node node, int cost,
                                                 JourneyStateUpdate journeyState) {
        journeyState.endWalk(node);
        return towardsStation.fromWalking(this, node, cost, journeyState);
    }

    @Override
    protected TraversalState toNoPlatformStation(NoPlatformStationState.Builder towardsStation, Node node, int cost,
                                                 JourneyStateUpdate journeyState) {
        journeyState.endWalk(node);
        return towardsStation.fromWalking(this, node, cost, journeyState);
    }

    @Override
    protected void toDestination(DestinationState.Builder towardsDestination, Node node, int cost,
                                 JourneyStateUpdate journeyState) {
        journeyState.endWalk(node);
        towardsDestination.from(this, cost);
    }
}
