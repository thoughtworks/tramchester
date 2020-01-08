package com.tramchester.graph.states;

import com.tramchester.graph.CachedNodeOperations;
import com.tramchester.graph.JourneyState;
import com.tramchester.graph.TransportGraphBuilder;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public abstract class TraversalState implements ImmuatableTraversalState {
    protected final Iterable<Relationship> outbounds;
    private final int costForLastEdge;
    private final int parentCost;
    protected final CachedNodeOperations nodeOperations;
    protected final long destinationNodeId;
    protected final TraversalState parent;
    protected final String destinationStationdId;
    protected final boolean interchangesOnly;

    @Override
    public int hashCode() {
        return Objects.hash(parent);
    }

    protected TraversalState(TraversalState parent, CachedNodeOperations nodeOperations, Iterable<Relationship> outbounds,
                             long destinationNodeId, String destinationStationdId, int costForLastEdge, boolean interchangesOnly) {
        this.parent = parent;
        this.nodeOperations = nodeOperations;
        this.outbounds = outbounds;
        this.destinationNodeId = destinationNodeId;
        this.destinationStationdId = destinationStationdId;
        this.costForLastEdge = costForLastEdge;
        this.interchangesOnly = interchangesOnly;
        parentCost = 0;
    }

    protected TraversalState(TraversalState parent, Iterable<Relationship> outbounds, int costForLastEdge) {
        this.nodeOperations = parent.nodeOperations;
        this.destinationNodeId = parent.destinationNodeId;
        this.destinationStationdId = parent.destinationStationdId;
        this.interchangesOnly = parent.interchangesOnly;

        this.parent = parent;
        this.outbounds = outbounds;
        this.costForLastEdge = costForLastEdge;
        this.parentCost = parent.getTotalCost();
    }

    public abstract TraversalState nextState(Path path, TransportGraphBuilder.Labels nodeLabel, Node node,
                                             JourneyState journeyState, int cost);

    public Iterable<Relationship> getOutbounds() {
        return outbounds;
    }

    protected List<Relationship> filterExcludingEndNode(Iterable<Relationship> relationships, long nodeIdToSkip) {

        return  StreamSupport.stream(relationships.spliterator(), false).
                filter(relationship -> relationship.getEndNode().getId()!= nodeIdToSkip).
                collect(Collectors.toList());
    }

    public int getTotalCost() {
        return parentCost + getCurrentCost();
    }

    protected int getCurrentCost() {
        return costForLastEdge;
    }

    protected List<Relationship> filterByTripId(Iterable<Relationship> relationships, String tripId) {
        List<Relationship> results = new ArrayList<>();
        relationships.forEach(relationship -> {
            String trips = nodeOperations.getTrips(relationship);
            if (trips.contains(tripId)) {
                results.add(relationship);
            }
        });
        return results;
    }

}
