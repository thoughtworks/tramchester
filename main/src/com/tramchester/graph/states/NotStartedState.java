package com.tramchester.graph.states;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.graph.CachedNodeOperations;
import com.tramchester.graph.JourneyState;
import com.tramchester.graph.TransportGraphBuilder;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.RelationshipType;

import java.util.ArrayList;
import java.util.List;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static org.neo4j.graphdb.Direction.OUTGOING;

// TODO seperate class for Bus?
public class NotStartedState extends TraversalState {

    public NotStartedState(CachedNodeOperations nodeOperations, long destinationNodeId, List<String> destinationStationIds,
                           TramchesterConfig config) {
        super(null, nodeOperations, new ArrayList<>(), destinationNodeId, destinationStationIds, 0, config);
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
                if (config.getBus()) {
                    return new StationState(this, firstNode.getRelationships(OUTGOING, INTERCHANGE_BOARD, BOARD, WALKS_FROM), cost,
                            firstNode.getId());
                } else {
                    return new StationState(this, firstNode.getRelationships(OUTGOING, ENTER_PLATFORM, WALKS_FROM), cost,
                            firstNode.getId());
                }
        }
        throw new RuntimeException("Unexpected node type: " + nodeLabel);
    }

}
