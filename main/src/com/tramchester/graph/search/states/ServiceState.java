package com.tramchester.graph.search.states;

import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.search.JourneyState;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

import static com.tramchester.graph.TransportRelationshipTypes.TO_HOUR;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class ServiceState extends TraversalState {

    public static class Builder {

        public TraversalState fromRouteStation(RouteStationState routeStationState, String tripId, Node node, int cost) {
            Iterable<Relationship> serviceRelationships = node.getRelationships(OUTGOING, TO_HOUR);
            return new ServiceState(routeStationState, serviceRelationships, ExistingTrip.onTrip(tripId), cost);
        }

        public TraversalState fromRouteStation(RouteStationStateJustBoarded justBoarded, Node node, int cost) {
            Iterable<Relationship> serviceRelationships = node.getRelationships(OUTGOING, TO_HOUR);
            return new ServiceState(justBoarded, serviceRelationships, ExistingTrip.none(), cost);
        }

        public TraversalState fromRouteStation(RouteStationStateEndTrip endTrip, Node node, int cost) {
            Iterable<Relationship> serviceRelationships = node.getRelationships(OUTGOING, TO_HOUR);
            return new ServiceState(endTrip, serviceRelationships, ExistingTrip.none(), cost);
        }
    }

    private final ExistingTrip maybeExistingTrip;

    private ServiceState(TraversalState parent, Iterable<Relationship> relationships, ExistingTrip maybeExistingTrip,
                         int cost) {
        super(parent, relationships, cost);
        this.maybeExistingTrip = maybeExistingTrip;
    }

    @Override
    public String toString() {
        return "ServiceState{" +
                "maybeExistingTrip=" + maybeExistingTrip +
                "} " + super.toString();
    }

    @Override
    public TraversalState createNextState(Path path, GraphBuilder.Labels nodeLabel, Node node, JourneyState journeyState, int cost) {
        if (nodeLabel == GraphBuilder.Labels.HOUR) {
            return builders.hour.FromService(this, node, cost, maybeExistingTrip);
        }
        throw new RuntimeException("Unexpected node type: "+nodeLabel);
    }

}
