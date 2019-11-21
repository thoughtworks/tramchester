package com.tramchester.graph.states;

import com.tramchester.graph.*;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class NotStartedState extends TraversalState {

    public NotStartedState(CachedNodeOperations nodeOperations, long destinationNodeId) {
        super(null, nodeOperations, new ArrayList<>(), destinationNodeId);
    }

    @Override
    public String toString() {
        return "NotStartedState{}";
    }

    public TraversalState nextState(Path path, TransportGraphBuilder.Labels nodeLabel, Node node, JourneyState journeyState) {
        switch(nodeLabel) {
            case QUERY_NODE:
                return new WalkingState(this, nodeOperations, costOrdered(node.getRelationships(OUTGOING, WALKS_TO)), destinationNodeId);
            case STATION: // we walked here
                return new StationState(this, nodeOperations, node.getRelationships(OUTGOING, ENTER_PLATFORM), destinationNodeId);
        }
        throw new RuntimeException("Unexpected node type: " + nodeLabel);
    }

    private Iterable<Relationship> costOrdered(Iterable<Relationship> outboundRelationships) {
        SortedMap<Integer, Relationship> ordered = new TreeMap<>();
        for (Relationship outboundRelationship : outboundRelationships) {
            int cost = (int) outboundRelationship.getProperty(GraphStaticKeys.COST);
            ordered.put(cost,outboundRelationship);
        }
        return ordered.values();
    }
}
