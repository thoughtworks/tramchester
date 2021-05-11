package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.search.JourneyState;
import com.tramchester.graph.search.stateMachine.ExistingTrip;
import com.tramchester.graph.search.stateMachine.RegistersFromState;
import com.tramchester.graph.search.stateMachine.Towards;
import com.tramchester.graph.search.stateMachine.UnexpectedNodeTypeException;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import static com.tramchester.graph.TransportRelationshipTypes.TO_MINUTE;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class HourState extends TraversalState {

    public static class Builder implements Towards<HourState> {

        public HourState fromService(ServiceState serviceState, Node node, int cost, ExistingTrip maybeExistingTrip) {
            Iterable<Relationship> relationships = node.getRelationships(OUTGOING, TO_MINUTE);
            return new HourState(serviceState, relationships, maybeExistingTrip, cost);
        }

        @Override
        public void register(RegistersFromState registers) {
            registers.add(ServiceState.class, this);
        }

        @Override
        public Class<HourState> getDestination() {
            return HourState.class;
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
        throw new UnexpectedNodeTypeException(node, "Unexpected node type: "+nodeLabel);
    }

    @Override
    protected TraversalState toMinute(MinuteState.Builder towardsMinute, Node node, int cost, JourneyState journeyState) {
        try {
            TramTime time = traversalOps.getTimeFrom(node);
            journeyState.recordVehicleDetails(time, getTotalCost());
        } catch (TramchesterException exception) {
            throw new RuntimeException("Unable to process time ordering", exception);
        }

        return towardsMinute.fromHour(this, node, cost, maybeExistingTrip);
    }

    @Override
    public String toString() {
        return "HourState{" +
                "maybeExistingTrip=" + maybeExistingTrip +
                "} " + super.toString();
    }
}
