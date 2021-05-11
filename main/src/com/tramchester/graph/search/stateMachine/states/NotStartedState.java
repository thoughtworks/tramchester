package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.search.JourneyState;
import com.tramchester.graph.search.stateMachine.TraversalOps;
import com.tramchester.graph.search.stateMachine.UnexpectedNodeTypeException;
import org.neo4j.graphdb.Node;

import java.util.Set;

public class NotStartedState extends TraversalState {

    public NotStartedState(TraversalOps traversalOps, TraversalStateFactory traversalStateFactory) {
        super(traversalOps, traversalStateFactory);
    }

    @Override
    public String toString() {
        return "NotStartedState{}";
    }

    @Override
    public int getTotalCost() {
        return 0;
    }

    public TraversalState createNextState(Set<GraphBuilder.Labels> nodeLabels, Node firstNode, JourneyState journeyState, int cost) {
        // should only be called for multi-mode stations
        return builders.towardsStation(this, NoPlatformStationState.class).fromStart(this, firstNode, cost);
    }

    public TraversalState createNextState(GraphBuilder.Labels nodeLabel, Node firstNode, JourneyState journeyState, int cost) {
        throw new UnexpectedNodeTypeException(firstNode, "Unexpected node type: " + nodeLabel);

//        switch(nodeLabel) {
//            case QUERY_NODE:
//                return builders.towardsWalk(this).fromStart(this, firstNode, cost);
//            case TRAM_STATION:
//                return builders.towardsStation(this, TramStationState.class).fromStart(this, firstNode, cost);
//            case BUS_STATION:
//            case TRAIN_STATION:
//            case FERRY_STATION:
//            case SUBWAY_STATION:
//                return builders.towardsStation(this, NoPlatformStationState.class).fromStart(this, firstNode, cost);
//            case GROUPED:
//                return builders.towardsGroup(this).fromStart(this, firstNode, cost);
//        }
//        throw new UnexpectedNodeTypeException(firstNode, "Unexpected node type: " + nodeLabel);
    }

    @Override
    protected TraversalState toWalk(WalkingState.Builder towardsWalk, Node node, int cost, JourneyState journeyState) {
        return towardsWalk.fromStart(this, node, cost);
    }

    @Override
    protected TraversalState toGrouped(GroupedStationState.Builder towardsGroup, Node node, int cost, JourneyState journeyState) {
        return towardsGroup.fromStart(this, node, cost);
    }

    @Override
    protected TramStationState toTramStation(TramStationState.Builder towardsStation, Node node, int cost, JourneyState journeyState) {
        return towardsStation.fromStart(this, node, cost);
    }

    @Override
    protected TraversalState toNoPlatformStation(NoPlatformStationState.Builder towardsStation, Node node, int cost, JourneyState journeyState) {
        return towardsStation.fromStart(this, node, cost);
    }
}
