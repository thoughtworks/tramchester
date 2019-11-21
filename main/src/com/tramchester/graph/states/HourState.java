package com.tramchester.graph.states;

import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.CachedNodeOperations;
import com.tramchester.graph.JourneyState;
import com.tramchester.graph.TransportGraphBuilder;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

import java.time.LocalTime;

import static com.tramchester.graph.TransportRelationshipTypes.TRAM_GOES_TO;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class HourState extends TraversalState {

    public HourState(TraversalState parent, CachedNodeOperations nodeOperations, Iterable<Relationship> relationships, long destinationNodeId) {
        super(parent, nodeOperations, relationships, destinationNodeId);
    }

    @Override
    public TraversalState nextState(Path path, TransportGraphBuilder.Labels nodeLabel, Node node, JourneyState journeyState) {
        try {
            if (nodeLabel == TransportGraphBuilder.Labels.MINUTE) {
                LocalTime time = nodeOperations.getTime(node);
                String tripId = nodeOperations.getTrip(node);
                journeyState.recordTramDetails(time, getTotalCost(path), tripId);
                Iterable<Relationship> relationships = timeOrdered(node.getRelationships(OUTGOING, TRAM_GOES_TO));
                return new MinuteState(this, nodeOperations, relationships,
                        tripId, destinationNodeId);
            }
        } catch(TramchesterException exception) {
            throw new RuntimeException("Unable to process time ordering", exception);
        }
        throw new RuntimeException("Unexpected node type: "+nodeLabel);
    }

    @Override
    public String toString() {
        return "HourState{" +
                "parent=" + parent +
                '}';
    }
}
