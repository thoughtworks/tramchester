package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.search.JourneyState;
import com.tramchester.graph.search.stateMachine.RegistersFromState;
import com.tramchester.graph.search.stateMachine.TowardsState;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.WALKS_TO;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class WalkingState extends TraversalState {

    public static class Builder implements TowardsState<WalkingState> {

        @Override
        public void register(RegistersFromState registers) {
            registers.add(NotStartedState.class, this);
            registers.add(NoPlatformStationState.class, this);
            registers.add(TraversalState.class, this);
            registers.add(TramStationState.class, this);
        }

        @Override
        public Class<WalkingState> getDestination() {
            return WalkingState.class;
        }

        public TraversalState fromStart(NotStartedState notStartedState, Node firstNode, int cost) {
            return new WalkingState(notStartedState, firstNode.getRelationships(OUTGOING, WALKS_TO), cost);
        }

        public TraversalState fromStation(StationState station, Node node, int cost) {
            return new WalkingState(station,
                    filterExcludingEndNode(node.getRelationships(OUTGOING), station), cost);
        }

    }

    private WalkingState(TraversalState parent, Stream<Relationship> relationships, int cost) {
        super(parent, relationships, cost);
    }

    private WalkingState(TraversalState parent, Iterable<Relationship> relationships, int cost) {
        super(parent, relationships, cost);
    }

    @Override
    public String toString() {
        return "WalkingState{} " + super.toString();
    }

    @Override
    public TraversalState createNextState(GraphBuilder.Labels nodeLabel, Node node, JourneyState journeyState, int cost) {
        // could be we've walked to our destination
        if (traversalOps.isDestination(node.getId())) {
            return builders.towardsDest(this).from(this, cost);
        }

        switch (nodeLabel) {
            case TRAM_STATION -> {
                journeyState.walkingConnection();
                return builders.towardsStation(this, TramStationState.class).fromWalking(this, node, cost);
            }
            case BUS_STATION, TRAIN_STATION -> {
                journeyState.walkingConnection();
                return builders.towardsStation(this, NoPlatformStationState.class).
                        fromWalking(this, node, cost);
            }
            case GROUPED -> {
                journeyState.walkingConnection();
                return builders.towardsGroup(this).fromWalk(this, node, cost);
            }
            default -> throw new RuntimeException("Unexpected node type: " + nodeLabel + " at " + this);
        }

    }
}
