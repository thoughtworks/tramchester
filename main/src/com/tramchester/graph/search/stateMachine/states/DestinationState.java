package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.graph.search.stateMachine.RegistersFromState;
import com.tramchester.graph.search.stateMachine.Towards;

import java.time.Duration;
import java.util.LinkedList;

/**
 * Used for the path to state mapping
 * The traversal code will stop before reaching here as it checks the destination node id's before invoking the next
 * state.
 */
public class DestinationState extends TraversalState
{
    public static class Builder implements Towards<DestinationState> {

        @Override
        public void register(RegistersFromState registers) {
            registers.add(NoPlatformStationState.class, this);
            registers.add(WalkingState.class, this);
            registers.add(PlatformStationState.class, this);
            registers.add(GroupedStationState.class, this);
        }

        @Override
        public Class<DestinationState> getDestination() {
            return DestinationState.class;
        }

        public DestinationState from(NoPlatformStationState noPlatformStation, Duration cost) {
            return new DestinationState(noPlatformStation, cost);
        }

        public DestinationState from(WalkingState walkingState, Duration cost) {
            return new DestinationState(walkingState, cost);
        }

        public DestinationState from(PlatformStationState platformStationState, Duration cost) {
            return new DestinationState(platformStationState, cost);
        }

        public DestinationState from(GroupedStationState groupedStationState, Duration cost) {
            return new DestinationState(groupedStationState, cost);
        }

    }

    private DestinationState(TraversalState parent, Duration cost) {
        super(parent, new LinkedList<>(), cost);
    }

    @Override
    public String toString() {
        return "DestinationState{} " + super.toString();
    }

}
