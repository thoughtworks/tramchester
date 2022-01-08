package com.tramchester.graph.search.stateMachine;

import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.states.GroupedStationState;
import com.tramchester.graph.search.stateMachine.states.NotStartedState;
import com.tramchester.graph.search.stateMachine.states.StationState;
import com.tramchester.graph.search.stateMachine.states.WalkingState;
import org.neo4j.graphdb.Node;

public interface TowardsStation<T extends StationState> extends Towards<T> {
    T fromNeighbour(StationState stationState, Node next, int cost, JourneyStateUpdate journeyState);
    T fromStart(NotStartedState notStartedState, Node firstNode, int cost, JourneyStateUpdate journeyState);
    T fromWalking(WalkingState walkingState, Node node, int cost, JourneyStateUpdate journeyState);
    T fromGrouped(GroupedStationState groupedStationState, Node next, int cost, JourneyStateUpdate journeyState);
}
