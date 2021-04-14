package com.tramchester.graph.search.states;

import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.search.JourneyState;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import static com.tramchester.graph.TransportRelationshipTypes.TO_MINUTE;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class HourState extends TraversalState {

    public static class Builder {
        public TraversalState FromService(ServiceState serviceState, Node node, int cost, ExistingTrip maybeExistingTrip) {
            Iterable<Relationship> relationships = node.getRelationships(OUTGOING, TO_MINUTE);
            return new HourState(serviceState, relationships, maybeExistingTrip, cost);
        }
    }

    private final ExistingTrip maybeExistingTrip;

    private HourState(TraversalState parent, Iterable<Relationship> relationships,
                      ExistingTrip maybeExistingTrip, int cost) {
        super(parent, relationships, cost);
        this.maybeExistingTrip = maybeExistingTrip;
    }

    @Override
    public TraversalState createNextState(GraphBuilder.Labels nodeLabel, Node node, JourneyState journeyState, int cost) {
        try {
            if (nodeLabel == GraphBuilder.Labels.MINUTE) {
                return toMinute(node, journeyState, cost);
            }
        } catch(TramchesterException exception) {
            throw new RuntimeException("Unable to process time ordering", exception);
        }
        throw new UnexpectedNodeTypeException(node, "Unexpected node type: "+nodeLabel);
    }

    private TraversalState toMinute(Node node, JourneyState journeyState, int cost) throws TramchesterException {
        TramTime time = nodeOperations.getTime(node);

        journeyState.recordVehicleDetails(time, getTotalCost());

        return builders.minute.fromHour(this, node, cost, maybeExistingTrip);
    }

    @Override
    public String toString() {
        return "HourState{" +
                "maybeExistingTrip=" + maybeExistingTrip +
                "} " + super.toString();
    }
}
