package com.tramchester.graph.states;

import com.tramchester.domain.TramTime;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.JourneyState;
import com.tramchester.graph.TransportGraphBuilder;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.tramchester.graph.TransportRelationshipTypes.TRAM_GOES_TO;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class HourState extends TraversalState {
//    private static final Logger logger = LoggerFactory.getLogger(HourState.class);

    private Optional<String> maybeExistingTrip;

    public HourState(TraversalState parent, Iterable<Relationship> relationships,
                     Optional<String> maybeExistingTrip, int cost) {
        super(parent, relationships, cost);
        this.maybeExistingTrip = maybeExistingTrip;
    }

    @Override
    public TraversalState nextState(Path path, TransportGraphBuilder.Labels nodeLabel, Node node, JourneyState journeyState, int cost) {
        try {
            if (nodeLabel == TransportGraphBuilder.Labels.MINUTE) {
                return toMinute(node, journeyState, cost);
            }
        } catch(TramchesterException exception) {
            throw new RuntimeException("Unable to process time ordering", exception);
        }
        throw new RuntimeException("Unexpected node type: "+nodeLabel);
    }

    private TraversalState toMinute(Node node, JourneyState journeyState, int cost) throws TramchesterException {
        TramTime time = nodeOperations.getTime(node);

        if (maybeExistingTrip.isPresent()) {
            // continuing an existing trip
            String existingTripId = maybeExistingTrip.get();
            journeyState.recordTramDetails(time, getTotalCost());
            Iterable<Relationship> relationships = filterBySingleTripId(node.getRelationships(OUTGOING, TRAM_GOES_TO), existingTripId);
            return new MinuteState(this, relationships, existingTripId, cost);
        } else {
            // starting a brand new journey
            Iterable<Relationship> relationships = node.getRelationships(OUTGOING, TRAM_GOES_TO);
            String tripId = nodeOperations.getTrip(node);
            journeyState.recordTramDetails(time, getTotalCost());
            return new MinuteState(this, relationships, tripId, cost);
        }
    }

    private List<Relationship> filterBySingleTripId(Iterable<Relationship> relationships, String tripId) {
        List<Relationship> results = new ArrayList<>();
        relationships.forEach(relationship -> {
            String trip = nodeOperations.getTrip(relationship);
            if (trip.equals(tripId)) {
                results.add(relationship);
            }
        });
        return results;
    }

    @Override
    public String toString() {
        return "HourState{" +
                "maybeExistingTrip=" + maybeExistingTrip +
                ", parent=" + parent +
                '}';
    }
}
