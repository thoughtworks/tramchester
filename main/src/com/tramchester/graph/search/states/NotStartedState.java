package com.tramchester.graph.search.states;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.geo.SortsPositions;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.NodeContentsRepository;
import com.tramchester.graph.search.JourneyState;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;

import java.util.ArrayList;
import java.util.List;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static org.neo4j.graphdb.Direction.OUTGOING;

// TODO seperate class for Bus?
public class NotStartedState extends TraversalState {
    private final BusStationState.Builder busStationStateBuilder;
    private final WalkingState.Builder walkingStateBuilder;
    private final TramStationState.Builder tramStationStateBuilder;

    public NotStartedState(SortsPositions sortsPositions, NodeContentsRepository nodeOperations, long destinationNodeId,
                           List<String> destinationStationIds,
                           TramchesterConfig config) {
        super(sortsPositions, null, nodeOperations, new ArrayList<>(), destinationNodeId, destinationStationIds, 0, config);
        busStationStateBuilder = new BusStationState.Builder();
        walkingStateBuilder = new WalkingState.Builder();
        tramStationStateBuilder = new TramStationState.Builder();
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
                return walkingStateBuilder.fromStart(this, firstNode, cost);
            case TRAM_STATION:
                return tramStationStateBuilder.fromStart(this, firstNode, cost);
            case BUS_STATION:
                return busStationStateBuilder.fromStart(this, firstNode, cost);
        }
        throw new RuntimeException("Unexpected node type: " + nodeLabel);
    }

}
