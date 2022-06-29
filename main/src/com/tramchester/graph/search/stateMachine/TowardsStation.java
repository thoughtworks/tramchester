package com.tramchester.graph.search.stateMachine;

import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.states.GroupedStationState;
import com.tramchester.graph.search.stateMachine.states.NotStartedState;
import com.tramchester.graph.search.stateMachine.states.StationState;
import com.tramchester.graph.search.stateMachine.states.WalkingState;
import org.neo4j.graphdb.Node;

import java.time.Duration;

public interface TowardsStation<T extends StationState> extends Towards<T> {
    T fromNeighbour(StationState stationState, Node next, Duration cost, JourneyStateUpdate journeyState, boolean onDiversion);
    T fromStart(NotStartedState notStartedState, Node firstNode, Duration cost, JourneyStateUpdate journeyState, boolean alreadyOnDiversion, boolean onDiversion);
    T fromWalking(WalkingState walkingState, Node node, Duration cost, JourneyStateUpdate journeyState);
    T fromGrouped(GroupedStationState groupedStationState, Node next, Duration cost, JourneyStateUpdate journeyState);
}
