package com.tramchester.graph.search.states;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.geo.SortsPositions;
import com.tramchester.graph.NodeContentsRepository;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.search.JourneyState;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;

import java.util.Set;

public class NotStartedState extends TraversalState {

    public NotStartedState(SortsPositions sortsPositions, NodeContentsRepository nodeOperations, long destinationNodeId,
                           Set<String> destinationStationIds,
                           TramchesterConfig config) {
        super(sortsPositions, nodeOperations, destinationNodeId, destinationStationIds, config);
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
                return builders.walking.fromStart(this, firstNode, cost);
            case TRAM_STATION:
                return builders.tramStation.fromStart(this, firstNode, cost);
            case BUS_STATION:
                return builders.busStation.from(this, firstNode, cost);
        }
        throw new RuntimeException("Unexpected node type: " + nodeLabel);
    }

}
