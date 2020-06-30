package com.tramchester.graph.search.states;

import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.search.JourneyState;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

import static com.tramchester.graph.TransportRelationshipTypes.TO_HOUR;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class ServiceState extends TraversalState {

    private final ExistingTrip maybeExistingTrip;

    private ServiceState(TraversalState parent, Iterable<Relationship> relationships, ExistingTrip maybeExistingTrip,
                         int cost) {
        super(parent, relationships, cost);
        this.maybeExistingTrip = maybeExistingTrip;
    }

    public static TraversalState fromRouteStation(RouteStationState routeStationState, ExistingTrip maybeExistingTrip, Node node, int cost) {
        Iterable<Relationship> serviceRelationships = node.getRelationships(OUTGOING, TO_HOUR);
        return new ServiceState(routeStationState, serviceRelationships, maybeExistingTrip, cost);
    }

    @Override
    public String toString() {
        return "ServiceState{" +
                "maybeExistingTrip=" + maybeExistingTrip +
                ", parent=" + parent +
                '}';
    }

    @Override
    public TraversalState createNextState(Path path, GraphBuilder.Labels nodeLabel, Node node, JourneyState journeyState, int cost) {
        if (nodeLabel == GraphBuilder.Labels.HOUR) {
            return HourState.FromService(this, maybeExistingTrip, node, cost);
        }
        throw new RuntimeException("Unexpected node type: "+nodeLabel);
    }

}
