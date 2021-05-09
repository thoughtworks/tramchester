package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.search.JourneyState;
import com.tramchester.graph.search.stateMachine.ExistingTrip;
import com.tramchester.graph.search.stateMachine.RegistersFromState;
import com.tramchester.graph.search.stateMachine.TowardsState;
import com.tramchester.graph.search.stateMachine.UnexpectedNodeTypeException;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import static com.tramchester.graph.TransportRelationshipTypes.TO_HOUR;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class ServiceState extends TraversalState {

    public static class Builder implements TowardsState<ServiceState> {

        @Override
        public void register(RegistersFromState registers) {
            registers.add(RouteStationStateOnTrip.class, this);
            registers.add(RouteStationStateEndTrip.class, this);
            registers.add(RouteStationStateJustBoarded.class, this);
        }

        @Override
        public Class<ServiceState> getDestination() {
            return ServiceState.class;
        }

        public TraversalState fromRouteStation(RouteStationStateOnTrip state, IdFor<Trip> tripId, Node node, int cost) {
            Iterable<Relationship> serviceRelationships = node.getRelationships(OUTGOING, TO_HOUR);
            return new ServiceState(state, serviceRelationships, ExistingTrip.onTrip(tripId), cost);
        }

        public TraversalState fromRouteStation(RouteStationStateEndTrip endTrip, Node node, int cost) {
            Iterable<Relationship> serviceRelationships = node.getRelationships(OUTGOING, TO_HOUR);
            return new ServiceState(endTrip, serviceRelationships, cost);
        }

        public TraversalState fromRouteStation(RouteStationStateJustBoarded justBoarded, Node node, int cost) {
            Iterable<Relationship> serviceRelationships = node.getRelationships(OUTGOING, TO_HOUR);
            return new ServiceState(justBoarded, serviceRelationships, cost);
        }

    }

    private final ExistingTrip maybeExistingTrip;

    private ServiceState(TraversalState parent, Iterable<Relationship> relationships, ExistingTrip maybeExistingTrip,
                         int cost) {
        super(parent, relationships, cost);
        this.maybeExistingTrip = maybeExistingTrip;
    }

    private ServiceState(TraversalState parent, Iterable<Relationship> relationships, int cost) {
        super(parent, relationships, cost);
        this.maybeExistingTrip = ExistingTrip.none();
    }

    @Override
    public String toString() {
        return "ServiceState{" +
                "maybeExistingTrip=" + maybeExistingTrip +
                "} " + super.toString();
    }

    @Override
    public TraversalState createNextState(GraphBuilder.Labels nodeLabel, Node node, JourneyState journeyState, int cost) {
        if (nodeLabel == GraphBuilder.Labels.HOUR) {
            return builders.towardsHour(this).fromService(this, node, cost, maybeExistingTrip);
        }
        throw new UnexpectedNodeTypeException(node, "Unexpected node type: "+nodeLabel);
    }

}
