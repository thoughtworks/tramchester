package com.tramchester.graph.search.stateMachine;

import com.tramchester.graph.search.stateMachine.states.GroupedStationState;
import com.tramchester.graph.search.stateMachine.states.NotStartedState;
import com.tramchester.graph.search.stateMachine.states.StationState;
import com.tramchester.graph.search.stateMachine.states.WalkingState;
import org.neo4j.graphdb.Node;

public interface TowardsStationState<T extends StationState> extends TowardsState<T>{
    T fromNeighbour(StationState stationState, Node next, int cost);
    T fromStart(NotStartedState notStartedState, Node firstNode, int cost);
    T fromWalking(WalkingState walkingState, Node node, int cost);
    T fromGrouped(GroupedStationState groupedStationState, Node next, int cost);
}
