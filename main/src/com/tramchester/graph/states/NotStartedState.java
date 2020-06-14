package com.tramchester.graph.states;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.graph.CachedNodeOperations;
import com.tramchester.graph.GraphBuilder;
import com.tramchester.graph.search.JourneyState;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;

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

    public TraversalState createNextState(Path path, GraphBuilder.Labels nodeLabel, Node firstNode, JourneyState journeyState, int cost) {
        switch(nodeLabel) {
            case QUERY_NODE:
                return new WalkingState(this, firstNode.getRelationships(OUTGOING, WALKS_TO), cost);
            case TRAM_STATION:
                return new TramStationState(this, firstNode.getRelationships(OUTGOING, ENTER_PLATFORM, WALKS_FROM), cost,
                        firstNode.getId());
            case BUS_STATION:
                return new BusStationState(this, firstNode.getRelationships(OUTGOING, INTERCHANGE_BOARD, BOARD, WALKS_FROM), cost,
                        firstNode.getId());
        }
        throw new RuntimeException("Unexpected node type: " + nodeLabel);
    }

}
