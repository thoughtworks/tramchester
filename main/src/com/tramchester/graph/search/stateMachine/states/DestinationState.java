package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.graph.search.stateMachine.RegistersFromState;
import com.tramchester.graph.search.stateMachine.Towards;

import java.util.LinkedList;

public class DestinationState extends TraversalState
{
    public static class Builder implements Towards<DestinationState> {

        @Override
        public void register(RegistersFromState registers) {
            registers.add(NoPlatformStationState.class, this);
            registers.add(WalkingState.class, this);
            registers.add(TramStationState.class, this);
//            registers.add(PlatformState.class, this);
            registers.add(GroupedStationState.class, this);
        }

        @Override
        public Class<DestinationState> getDestination() {
            return DestinationState.class;
        }

        public DestinationState from(NoPlatformStationState noPlatformStation, int cost) {
            return new DestinationState(noPlatformStation, cost);
        }

        public DestinationState from(WalkingState walkingState, int cost) {
            return new DestinationState(walkingState, cost);
        }

        public DestinationState from(TramStationState tramStationState, int cost) {
            return new DestinationState(tramStationState, cost);
        }

//        public DestinationState from(PlatformState state, int cost) {
//            return new DestinationState(state, cost);
//        }

        public DestinationState from(GroupedStationState groupedStationState, int cost) {
            return new DestinationState(groupedStationState, cost);
        }

    }

    private DestinationState(TraversalState parent, int cost) {
        super(parent, new LinkedList<>(), cost);
    }

    @Override
    public String toString() {
        return "DestinationState{} " + super.toString();
    }

}
