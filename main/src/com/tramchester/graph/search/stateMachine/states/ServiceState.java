package com.tramchester.graph.search.stateMachine.states;

import com.google.common.collect.Streams;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.graph.search.stateMachine.ExistingTrip;
import com.tramchester.graph.search.stateMachine.RegistersFromState;
import com.tramchester.graph.search.stateMachine.Towards;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.Comparator;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.TO_HOUR;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class ServiceState extends TraversalState {

    public static class Builder implements Towards<ServiceState> {

        private final boolean depthFirst;

        public Builder(boolean depthFirst) {
            this.depthFirst = depthFirst;
        }

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
            Stream<Relationship> serviceRelationships = getHourRelationships(node);
            return new ServiceState(state, serviceRelationships, ExistingTrip.onTrip(tripId), cost);
        }

        public TraversalState fromRouteStation(RouteStationStateEndTrip endTrip, Node node, int cost) {
            Stream<Relationship> serviceRelationships = getHourRelationships(node);
            return new ServiceState(endTrip, serviceRelationships, cost);
        }

        public TraversalState fromRouteStation(JustBoardedState justBoarded, Node node, int cost) {
            Stream<Relationship> serviceRelationships = getHourRelationships(node);
            return new ServiceState(justBoarded, serviceRelationships, cost);
        }

        private Stream<Relationship> getHourRelationships(Node node) {
            Stream<Relationship> relationships = Streams.stream(node.getRelationships(OUTGOING, TO_HOUR));
            if (depthFirst) {
                return relationships.sorted(Comparator.comparingInt(GraphProps::getHour));
            }
            return relationships;
        }
    }

    private final ExistingTrip maybeExistingTrip;

    private ServiceState(TraversalState parent, Stream<Relationship> relationships, ExistingTrip maybeExistingTrip,
                         int cost) {
        super(parent, relationships, cost);
        this.maybeExistingTrip = maybeExistingTrip;
    }

    private ServiceState(TraversalState parent, Stream<Relationship> relationships, int cost) {
        super(parent, relationships, cost);
        this.maybeExistingTrip = ExistingTrip.none();
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
