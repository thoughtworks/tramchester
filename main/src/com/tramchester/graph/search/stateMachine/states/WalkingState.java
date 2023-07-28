package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.OptionalResourceIterator;
import com.tramchester.graph.search.stateMachine.RegistersFromState;
import com.tramchester.graph.search.stateMachine.Towards;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.internal.helpers.collection.Iterables;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.WALKS_TO_STATION;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class WalkingState extends TraversalState {

    public static class Builder implements Towards<WalkingState> {

        @Override
        public void register(RegistersFromState registers) {
            registers.add(TraversalStateType.NotStartedState, this);
            registers.add(TraversalStateType.NoPlatformStationState, this);
            registers.add(TraversalStateType.PlatformStationState, this);

            // todo needed?
            //registers.add(GroupedStationState.class, this);
        }

        @Override
        public TraversalStateType getDestination() {
            return TraversalStateType.WalkingState;
        }

        public TraversalState fromStart(NotStartedState notStartedState, Node firstNode, Duration cost) {
            final ResourceIterable<Relationship> relationships = firstNode.getRelationships(OUTGOING, WALKS_TO_STATION);
            OptionalResourceIterator<Relationship> towardsDest = notStartedState.traversalOps.getTowardsDestination(relationships);

            // prioritise a direct walk from start if one is available
            if (towardsDest.isEmpty()) {
                return new WalkingState(notStartedState, relationships, cost, this);
            } else {
                // direct
                return new WalkingState(notStartedState, Iterables.asResourceIterable(towardsDest), cost, this);
            }
        }

        public TraversalState fromStation(StationState station, Node node, Duration cost) {
            return new WalkingState(station,
                    filterExcludingEndNode(node.getRelationships(OUTGOING), station), cost, this);
        }

    }

    private WalkingState(TraversalState parent, Stream<Relationship> relationships, Duration cost, Towards<WalkingState> builder) {
        super(parent, relationships, cost, builder.getDestination());
    }

    private WalkingState(TraversalState parent, ResourceIterable<Relationship> relationships, Duration cost, Towards<WalkingState> builder) {
        super(parent, relationships, cost, builder.getDestination());
    }

    @Override
    public String toString() {
        return "WalkingState{} " + super.toString();
    }

    @Override
    protected PlatformStationState toPlatformStation(PlatformStationState.Builder towardsStation, Node node, Duration cost,
                                                     JourneyStateUpdate journeyState, boolean onDiversion) {
        journeyState.endWalk(node);
        return towardsStation.fromWalking(this, node, cost, journeyState);
    }

    @Override
    protected TraversalState toNoPlatformStation(NoPlatformStationState.Builder towardsStation, Node node, Duration cost,
                                                 JourneyStateUpdate journeyState, boolean onDiversion) {
        journeyState.endWalk(node);
        return towardsStation.fromWalking(this, node, cost, journeyState);
    }

    @Override
    protected void toDestination(DestinationState.Builder towardsDestination, Node node, Duration cost,
                                 JourneyStateUpdate journeyState) {
        journeyState.endWalk(node);
        towardsDestination.from(this, cost);
    }
}
