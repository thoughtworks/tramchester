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
            registers.add(TraversalStateType.NoPlatformStationState, this);
            registers.add(TraversalStateType.WalkingState, this);
            registers.add(TraversalStateType.PlatformStationState, this);
            registers.add(TraversalStateType.GroupedStationState, this);
        }

        @Override
        public TraversalStateType getDestination() {
            return TraversalStateType.DestinationState;
        }

        public DestinationState from(NoPlatformStationState noPlatformStation, Duration cost) {
            return new DestinationState(noPlatformStation, cost, this);
        }

        public DestinationState from(WalkingState walkingState, Duration cost) {
            return new DestinationState(walkingState, cost, this);
        }

        public DestinationState from(PlatformStationState platformStationState, Duration cost) {
            return new DestinationState(platformStationState, cost, this);
        }

        public DestinationState from(GroupedStationState groupedStationState, Duration cost) {
            return new DestinationState(groupedStationState, cost, this);
        }

    }

    private DestinationState(TraversalState parent, Duration cost, Towards<DestinationState> builder) {
        super(parent, new LinkedList<>(), cost, builder.getDestination());
    }

    @Override
    public String toString() {
        return "DestinationState{} " + super.toString();
    }

}
