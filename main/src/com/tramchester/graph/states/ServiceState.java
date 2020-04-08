package com.tramchester.graph.states;

import com.tramchester.graph.search.JourneyState;
import com.tramchester.graph.TransportGraphBuilder;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

import java.util.Optional;

import static com.tramchester.graph.TransportRelationshipTypes.TO_MINUTE;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class ServiceState extends TraversalState {

    private Optional<String> maybeExistingTrip;

    public ServiceState(TraversalState parent, Iterable<Relationship> relationships, Optional<String> maybeExistingTrip,
                        int cost) {
        super(parent, relationships, cost);
        this.maybeExistingTrip = maybeExistingTrip;
    }

    @Override
    public String toString() {
        return "ServiceState{" +
                "maybeExistingTrip=" + maybeExistingTrip +
                ", parent=" + parent +
                '}';
    }

    @Override
    public TraversalState createNextState(Path path, TransportGraphBuilder.Labels nodeLabel, Node node, JourneyState journeyState, int cost) {
        if (nodeLabel == TransportGraphBuilder.Labels.HOUR) {
            Iterable<Relationship> relationships = node.getRelationships(OUTGOING, TO_MINUTE);
            return new HourState(this, relationships, maybeExistingTrip, cost);
        }

        throw new RuntimeException("Unexpected node type: "+nodeLabel);
    }

}
