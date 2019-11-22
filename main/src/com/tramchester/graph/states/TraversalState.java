package com.tramchester.graph.states;

import com.google.common.collect.Streams;
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

public abstract class TraversalState {
    protected final Iterable<Relationship> relationships;
    protected final CachedNodeOperations nodeOperations;
    protected final long destinationNodeId;
    protected final TraversalState parent;
    protected final String destinationStationdId;

    @Override
    public int hashCode() {
        return Objects.hash(parent);
    }

    protected TraversalState(TraversalState parent, CachedNodeOperations nodeOperations, Iterable<Relationship> relationships,
                             long destinationNodeId, String destinationStationdId) {
        this.parent = parent;
        this.nodeOperations = nodeOperations;
        this.relationships = relationships;
        this.destinationNodeId = destinationNodeId;
        this.destinationStationdId = destinationStationdId;
    }

    protected TraversalState(TraversalState parent, Iterable<Relationship> relationships) {
        this.nodeOperations = parent.nodeOperations;
        this.destinationNodeId = parent.destinationNodeId;
        this.destinationStationdId = parent.destinationStationdId;
        this.parent = parent;
        this.relationships = relationships;
    }

    public abstract TraversalState nextState(Path path, TransportGraphBuilder.Labels nodeLabel, Node node,
                                      JourneyState journeyState);

    public Iterable<Relationship> getRelationships() {
        return relationships;
    }

    protected List<Relationship> filterExcludingEndNode(Iterable<Relationship> relationships, long nodeIdToSkip) {
        return Streams.stream(relationships).
                filter(relationship -> relationship.getEndNode().getId()!= nodeIdToSkip).
                collect(Collectors.toList());
    }

    protected Iterable<Relationship> hourOrdered(Iterable<Relationship> outboundRelationships) {
        SortedMap<Integer, Relationship> ordered = new TreeMap<>();
        for (Relationship outboundRelationship : outboundRelationships) {
            int hour = nodeOperations.getHour(outboundRelationship);
            ordered.put(hour,outboundRelationship);
        }
        return ordered.values();
    }

    protected Iterable<Relationship> timeOrdered(Iterable<Relationship> outboundRelationships) {
        SortedMap<TramTime, Relationship> ordered = new TreeMap<>();
        for (Relationship outboundRelationship : outboundRelationships) {
            LocalTime time = nodeOperations.getTime(outboundRelationship);
            ordered.put(TramTime.of(time),outboundRelationship);
        }
        return ordered.values();
    }

    protected int getTotalCost(Path path) {
        int result = 0;
        for (Relationship relat: path.relationships()) {
            result = result + nodeOperations.getCost(relat);
        }
        return result;
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
