package com.tramchester.graph.states;

import com.tramchester.graph.CachedNodeOperations;
import com.tramchester.graph.JourneyState;
import com.tramchester.graph.TransportGraphBuilder;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;

import java.util.LinkedList;

public class DestinationState extends TraversalState
{
    public DestinationState(TraversalState parent, CachedNodeOperations nodeOperations, long destinationNodeId) {
        super(parent, nodeOperations, new LinkedList<>(), destinationNodeId);
    }

    @Override
    public TraversalState nextState(Path path, TransportGraphBuilder.Labels nodeLabel, Node node, JourneyState journeyState) {
        throw new RuntimeException("Already at destination, id is " + destinationNodeId);
    }

    @Override
    public String toString() {
        return "DestinationState{" +
                "destinationNodeId=" + destinationNodeId +
                ", parent=" + parent +
                '}';
    }
}
