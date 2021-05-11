package com.tramchester.graph.search.stateMachine.states;

import com.google.common.collect.Streams;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.search.JourneyState;
import com.tramchester.graph.search.stateMachine.*;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.*;
import java.util.stream.Stream;

import static java.lang.String.format;

public abstract class TraversalState implements ImmuatableTraversalState {

    protected final TraversalStateFactory builders;
    protected final TraversalOps traversalOps;
    private final Iterable<Relationship> outbounds;
    private final int costForLastEdge;
    private final int parentCost;
    private final TraversalState parent;

    private TraversalState child;

    // initial only
    protected TraversalState(TraversalOps traversalOps, TraversalStateFactory traversalStateFactory) {
        this.traversalOps = traversalOps;
        this.builders = traversalStateFactory;

        this.costForLastEdge = 0;
        this.parentCost = 0;
        this.parent = null;
        this.outbounds = new ArrayList<>();
    }

    protected TraversalState(TraversalState parent, Stream<Relationship> outbounds, int costForLastEdge) {
        this(parent, outbounds::iterator, costForLastEdge);
    }

    protected TraversalState(TraversalState parent, Iterable<Relationship> outbounds, int costForLastEdge) {
        this.traversalOps = parent.traversalOps;
        this.builders = parent.builders;
        this.parent = parent;

        this.outbounds = outbounds;
        this.costForLastEdge = costForLastEdge;
        this.parentCost = parent.getTotalCost();
    }

    protected abstract TraversalState createNextState(GraphBuilder.Labels nodeLabel, Node node,
                                                      JourneyState journeyState, int cost);

    protected TraversalState createNextState(Set<GraphBuilder.Labels> nodeLabels, Node node,
                                             JourneyState journeyState, int cost) {
        throw new RuntimeException(format("Multi label Not implemented at %s for %s labels were %s",
                this, journeyState, nodeLabels));
    }

    public TraversalState nextStateLegacy(Set<GraphBuilder.Labels> nodeLabels, Node node,
                                    JourneyState journeyState, int cost) {
        if (nodeLabels.size()>1) {
            return createNextState(nodeLabels, node, journeyState, cost);
        }
        GraphBuilder.Labels nodeLabel = nodeLabels.iterator().next();
        return createNextState(nodeLabel, node, journeyState, cost);
    }

    public TraversalState nextState(Set<GraphBuilder.Labels> nodeLabels, Node node,
                                       JourneyState journeyState, int cost) {
        long nodeId = node.getId();

        if (nodeLabels.size()>1) {
            return createNextState(nodeLabels, node, journeyState, cost);
        }
        GraphBuilder.Labels nodeLabel = nodeLabels.iterator().next();
        switch (nodeLabel) {
            case HOUR -> { return toHour(builders.getTowardsHour(this.getClass()), node, cost); }
            case TRAM_STATION -> {
                if (traversalOps.isDestination(nodeId)) {
                    return toDestination(builders.getTowardsDestination(this.getClass()), cost);
                } else {
                    return toStation(builders.getTowardsStation(this.getClass()), node, cost, journeyState);
                }
            }
            default -> { return createNextState(nodeLabel, node, journeyState, cost); }
        }
    }

    protected TramStationState toStation(TramStationState.Builder towardsStation, Node node, int cost, JourneyState journeyState) {
        throw new RuntimeException("No such transition at " + this.getClass());
    }

    protected DestinationState toDestination(DestinationState.Builder towardsDestination, int cost) {
        throw new RuntimeException("No such transition at " + this.getClass());
    }

    protected HourState toHour(HourState.Builder towardsHour, Node node, int cost) {
        throw new RuntimeException("No such transition at " + this.getClass());
    }

    public void dispose() {
        if (child!=null) {
            child.dispose();
            child = null;
        }
    }

    public Iterable<Relationship> getOutbounds() {
        return outbounds;
    }

    protected static Stream<Relationship> filterExcludingEndNode(Iterable<Relationship> relationships, NodeId hasNodeId) {
        long nodeId = hasNodeId.nodeId();
        return Streams.stream(relationships).
                filter(relationship -> relationship.getEndNode().getId() != nodeId);
    }

    public int getTotalCost() {
        return parentCost + getCurrentCost();
    }

    private int getCurrentCost() {
        return costForLastEdge;
    }

    @Override
    public String toString() {
        return "TraversalState{" +
                "costForLastEdge=" + costForLastEdge +
                ", parentCost=" + parentCost + System.lineSeparator() +
                ", parent=" + parent +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(parent);
    }

}
