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

        public TraversalState fromRouteStation(RouteStationState routeStationState, ExistingTrip maybeExistingTrip, Node node, int cost) {
            Iterable<Relationship> serviceRelationships = node.getRelationships(OUTGOING, TO_HOUR);
            return new ServiceState(routeStationState, serviceRelationships, maybeExistingTrip, cost);
        }
    }

    private final ExistingTrip maybeExistingTrip;
    private final HourState.Builder builder;

    private ServiceState(TraversalState parent, Iterable<Relationship> relationships, ExistingTrip maybeExistingTrip,
                         int cost) {
        super(parent, relationships, cost);
        builder = new HourState.Builder();
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
    public TraversalState createNextState(Path path, GraphBuilder.Labels nodeLabel, Node node, JourneyState journeyState, int cost) {
        if (nodeLabel == GraphBuilder.Labels.HOUR) {
            return builder.FromService(this, maybeExistingTrip, node, cost);
        }
        throw new RuntimeException("Unexpected node type: "+nodeLabel);
    }

}
