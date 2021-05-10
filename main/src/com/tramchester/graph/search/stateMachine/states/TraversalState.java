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
    protected TraversalState(TraversalOps traversalOps) {
        this.traversalOps = traversalOps;
        this.builders = traversalOps.getBuilders();

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
        this.builders = traversalOps.getBuilders();
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

    public TraversalState nextState(Set<GraphBuilder.Labels> nodeLabels, Node node,
                                    JourneyState journeyState, int cost) {
        if (nodeLabels.size()==1) {
            GraphBuilder.Labels nodeLabel = nodeLabels.iterator().next();
            child = createNextState(nodeLabel, node, journeyState, cost);
        } else {
            child = createNextState(nodeLabels, node, journeyState, cost);
        }

        return child;
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
