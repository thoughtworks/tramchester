package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.search.JourneyState;
import com.tramchester.graph.search.stateMachine.TraversalOps;
import com.tramchester.graph.search.stateMachine.UnexpectedNodeTypeException;
import org.neo4j.graphdb.Node;

import java.util.Set;

public class NotStartedState extends TraversalState {

    public NotStartedState(TraversalOps traversalOps) {
        super(traversalOps);
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
        return builders.towardsNeighbour(this, NoPlatformStationState.class).from(this, firstNode, cost);
    }

    public TraversalState createNextState(GraphBuilder.Labels nodeLabel, Node firstNode, JourneyState journeyState, int cost) {
        switch(nodeLabel) {
            case QUERY_NODE:
                return builders.walking.fromStart(this, firstNode, cost);
            case TRAM_STATION:
                return builders.tramStation.fromStart(this, firstNode, cost);
            case BUS_STATION:
            case TRAIN_STATION:
            case FERRY_STATION:
            case SUBWAY_STATION:
                return builders.towardsNeighbour(this, NoPlatformStationState.class).
                        from(this, firstNode, cost);
            case GROUPED:
                return builders.groupedStation.fromStart(this, firstNode, cost);
        }
        throw new UnexpectedNodeTypeException(firstNode, "Unexpected node type: " + nodeLabel);
    }

}
