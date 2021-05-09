package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.search.JourneyState;
import com.tramchester.graph.search.stateMachine.RegistersFromState;
import com.tramchester.graph.search.stateMachine.TowardsState;
import org.neo4j.graphdb.Node;

import java.util.LinkedList;

public class DestinationState extends TraversalState
{
    public static class Builder implements TowardsState<DestinationState> {

        @Override
        public void register(RegistersFromState registers) {
            registers.add(NoPlatformStationState.class, this);
            registers.add(WalkingState.class, this);
            registers.add(TramStationState.class, this);
            registers.add(PlatformState.class, this);
            registers.add(RouteStationStateOnTrip.class, this);
            registers.add(RouteStationStateEndTrip.class, this);
            registers.add(GroupedStationState.class, this);
        }

        @Override
        public Class<DestinationState> getDestination() {
            return DestinationState.class;
        }

        public TraversalState from(NoPlatformStationState noPlatformStation, int cost) {
            return new DestinationState(noPlatformStation, cost);
        }

        public TraversalState from(WalkingState walkingState, int cost) {
            return new DestinationState(walkingState, cost);
        }

        public TraversalState from(TramStationState tramStationState, int cost) {
            return new DestinationState(tramStationState, cost);
        }

        public TraversalState from(PlatformState platformState, int cost) {
            return new DestinationState(platformState, cost);
        }

        public TraversalState from(RouteStationStateOnTrip routeStationStateOnTrip, int cost) {
            return new DestinationState(routeStationStateOnTrip, cost);
        }

        public TraversalState from(RouteStationStateEndTrip endTrip, int cost) {
            return new DestinationState(endTrip, cost);
        }

        public TraversalState from(GroupedStationState groupedStationState, int cost) {
            return new DestinationState(groupedStationState, cost);
        }

    }

    private DestinationState(TraversalState parent, int cost) {
        super(parent, new LinkedList<>(), cost);
    }

//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (!(o instanceof DestinationState)) return false;
//        TraversalState that = (TraversalState) o;
//        return that.destinationNodeIds == this.destinationNodeIds;
//    }

    @Override
    public TraversalState createNextState(GraphBuilder.Labels nodeLabel, Node node, JourneyState journeyState, int cost) {
        throw new RuntimeException("Already at destination, id is " + node.getId());
    }

    @Override
    public String toString() {
        return "DestinationState{} " + super.toString();
    }
}
