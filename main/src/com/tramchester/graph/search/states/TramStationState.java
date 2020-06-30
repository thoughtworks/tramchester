package com.tramchester.graph.search.states;

import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.search.JourneyState;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class TramStationState extends TraversalState {

    public static class Builder {
        public TraversalState fromWalking(WalkingState walkingState, Node node, int cost) {
            return new TramStationState(walkingState, node.getRelationships(OUTGOING, ENTER_PLATFORM), cost, node.getId());
        }

        public TraversalState fromPlatform(PlatformState platformState, Node node, int cost) {
            return new TramStationState(platformState,
                    filterExcludingEndNode(node.getRelationships(OUTGOING, ENTER_PLATFORM, WALKS_FROM), platformState),
                    cost, node.getId());
        }

        public TraversalState fromStart(NotStartedState notStartedState, Node node, int cost) {
            return new TramStationState(notStartedState, node.getRelationships(OUTGOING, ENTER_PLATFORM, WALKS_FROM), cost,
                    node.getId());
        }
    }

    private final WalkingState.Builder walkingStateBuilder;
    private final PlatformState.Builder platformStateBuilder;

    private final long stationNodeId;

    private TramStationState(TraversalState parent, Iterable<Relationship> relationships, int cost, long stationNodeId) {
        super(parent, relationships, cost);
        this.stationNodeId = stationNodeId;
        walkingStateBuilder = new WalkingState.Builder();
        platformStateBuilder = new PlatformState.Builder();
    }

    @Override
    public String toString() {
        return "TramStationState{" +
                "stationNodeId=" + stationNodeId +
                ", cost=" + super.getCurrentCost() +
                ", parent=" + parent +
                '}';
    }

    @Override
    public TraversalState createNextState(Path path, GraphBuilder.Labels nodeLabel, Node node,
                                          JourneyState journeyState, int cost) {
        long nodeId = node.getId();
        if (nodeId == destinationNodeId) {
            // TODO Cost of platform depart?
            return new DestinationState(this, cost);
        }

        if (nodeLabel == GraphBuilder.Labels.PLATFORM) {
            return platformStateBuilder.from(this, node, cost);

        }
        if (nodeLabel == GraphBuilder.Labels.QUERY_NODE) {
            return walkingStateBuilder.fromTramStation(this, node, cost);
        }

        throw new RuntimeException("Unexpected node type: "+nodeLabel);

    }
}
