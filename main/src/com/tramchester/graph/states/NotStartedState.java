package com.tramchester.graph.states;

import com.tramchester.graph.CachedNodeOperations;
import com.tramchester.graph.JourneyState;
import com.tramchester.graph.TransportGraphBuilder;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;

import java.util.ArrayList;
import java.util.List;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class NotStartedState extends TraversalState {

    public NotStartedState(CachedNodeOperations nodeOperations, long destinationNodeId, List<String> destinationStationIds,
                           boolean interchangesOnly) {
        super(null, nodeOperations, new ArrayList<>(), destinationNodeId, destinationStationIds, 0, interchangesOnly);
    }

    @Override
    public String toString() {
        return "NotStartedState{}";
    }

    @Override
    public int getTotalCost() {
        return 0;
    }

    public TraversalState nextState(Path path, TransportGraphBuilder.Labels nodeLabel, Node firstNode, JourneyState journeyState, int cost) {
        switch(nodeLabel) {
            case QUERY_NODE:
                return new WalkingState(this, firstNode.getRelationships(OUTGOING, WALKS_TO), cost);
            case STATION:
                return new StationState(this, firstNode.getRelationships(OUTGOING, ENTER_PLATFORM, WALKS_FROM), cost,
                        firstNode.getId());
        }
        throw new RuntimeException("Unexpected node type: " + nodeLabel);
    }

}
