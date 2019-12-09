package com.tramchester.graph.states;

import com.tramchester.domain.TramTime;
import com.tramchester.graph.CachedNodeOperations;
import com.tramchester.graph.JourneyState;
import com.tramchester.graph.TransportGraphBuilder;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public abstract class TraversalState {
    protected final Iterable<Relationship> outbounds;
    private final int costForLastEdge;
    private final int parentCost;
    protected final CachedNodeOperations nodeOperations;
    protected final long destinationNodeId;
    protected final TraversalState parent;
    protected final String destinationStationdId;

    @Override
    public int hashCode() {
        return Objects.hash(parent);
    }

    protected TraversalState(TraversalState parent, CachedNodeOperations nodeOperations, Iterable<Relationship> outbounds,
                             long destinationNodeId, String destinationStationdId, int costForLastEdge) {
        this.parent = parent;
        this.nodeOperations = nodeOperations;
        this.outbounds = outbounds;
        this.destinationNodeId = destinationNodeId;
        this.destinationStationdId = destinationStationdId;
        this.costForLastEdge = costForLastEdge;
        parentCost = 0;
    }

    protected TraversalState(TraversalState parent, Iterable<Relationship> outbounds, int costForLastEdge) {
        this.nodeOperations = parent.nodeOperations;
        this.destinationNodeId = parent.destinationNodeId;
        this.destinationStationdId = parent.destinationStationdId;

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

//    protected Iterable<Relationship> hourOrdered(Iterable<Relationship> outboundRelationships) {
//        SortedMap<Integer, Relationship> ordered = new TreeMap<>();
//        for (Relationship outboundRelationship : outboundRelationships) {
//            int hour = nodeOperations.getHour(outboundRelationship);
//            ordered.put(hour,outboundRelationship);
//        }
//        return ordered.values();
//    }

    protected Iterable<Relationship> timeOrdered(Iterable<Relationship> outboundRelationships) {
        SortedMap<TramTime, Relationship> ordered = new TreeMap<>();
        for (Relationship outboundRelationship : outboundRelationships) {
            LocalTime time = nodeOperations.getTime(outboundRelationship);
            ordered.put(TramTime.of(time),outboundRelationship);
        }
        return ordered.values();
    }

    public int getTotalCost() {
        return parentCost + getCurrentCost();
//        int result = 0;
//        for (Relationship relat: path.relationships()) {
//            result = result + nodeOperations.getCost(relat);
//        }
//        return result;
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
