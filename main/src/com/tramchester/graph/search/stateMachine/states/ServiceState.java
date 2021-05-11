package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.search.JourneyState;
import com.tramchester.graph.search.stateMachine.ExistingTrip;
import com.tramchester.graph.search.stateMachine.RegistersFromState;
import com.tramchester.graph.search.stateMachine.Towards;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import static com.tramchester.graph.TransportRelationshipTypes.TO_HOUR;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class ServiceState extends TraversalState {

    public static class Builder implements Towards<ServiceState> {

        @Override
        public void register(RegistersFromState registers) {
            registers.add(RouteStationStateOnTrip.class, this);
            registers.add(RouteStationStateEndTrip.class, this);
            registers.add(JustBoardedState.class, this);
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

        public TraversalState fromRouteStation(JustBoardedState justBoarded, Node node, int cost) {
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
    protected TraversalState createNextState(GraphBuilder.Labels nodeLabel, Node node, JourneyState journeyState, int cost) {
        throw new RuntimeException("no longer used");
    }

    @Override
    protected HourState toHour(HourState.Builder towardsHour, Node node, int cost) {
        return towardsHour.fromService(this, node, cost, maybeExistingTrip);
    }

    @Override
    public String toString() {
        return "ServiceState{" +
                "maybeExistingTrip=" + maybeExistingTrip +
                "} " + super.toString();
    }

}
