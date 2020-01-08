package com.tramchester.graph.states;

import com.tramchester.graph.CachedNodeOperations;
import com.tramchester.graph.GraphStaticKeys;
import com.tramchester.graph.JourneyState;
import com.tramchester.graph.TransportGraphBuilder;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.tramchester.graph.TransportRelationshipTypes.ENTER_PLATFORM;
import static com.tramchester.graph.TransportRelationshipTypes.WALKS_TO;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class NotStartedState extends TraversalState {

    public NotStartedState(CachedNodeOperations nodeOperations, long destinationNodeId, String destinationStationId,
                           boolean interchangesOnly) {
        super(null, nodeOperations, new ArrayList<>(), destinationNodeId, destinationStationId, 0, interchangesOnly);
    }

    @Override
    public String toString() {
        return "NotStartedState{}";
    }

    @Override
    public int getTotalCost() {
        return 0;
    }

    public TraversalState nextState(Path path, TransportGraphBuilder.Labels nodeLabel, Node node, JourneyState journeyState, int cost) {
        switch(nodeLabel) {
            case QUERY_NODE:
                return new WalkingState(this, costOrdered(node.getRelationships(OUTGOING, WALKS_TO)), cost);
            case STATION:
                return new StationState(this, node.getRelationships(OUTGOING, ENTER_PLATFORM), cost);
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
