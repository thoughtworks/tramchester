package com.tramchester.graph.states;

import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.GraphStaticKeys;
import com.tramchester.graph.JourneyState;
import com.tramchester.graph.TransportGraphBuilder;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

import java.util.Collections;
import java.util.Optional;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class RouteStationState extends TraversalState {
    private final long routeStationNodeId;
    private final boolean justBoarded;
    private Optional<String> maybeExistingTrip;

    public RouteStationState(TraversalState parent, Iterable<Relationship> relationships, long routeStationNodeId, int cost) {
        super(parent, relationships, cost);
        this.routeStationNodeId = routeStationNodeId;
        this.justBoarded = true;
        maybeExistingTrip = Optional.empty();
    }

    public RouteStationState(TraversalState parent, Iterable<Relationship> relationships,
                             long routeStationNodeId, String tripId, int cost) {
        super(parent, relationships, cost);
        this.routeStationNodeId = routeStationNodeId;
        this.justBoarded = false;
        maybeExistingTrip = Optional.of(tripId);
    }

    @Override
    public String toString() {
        return "RouteStationState{" +
                "routeStationNodeId=" + routeStationNodeId +
                ", justBoarded=" + justBoarded +
                ", maybeExistingTrip='" + maybeExistingTrip + '\'' +
                ", parent=" + parent +
                '}';
    }

    @Override
    public TraversalState nextState(Path path, TransportGraphBuilder.Labels nodeLabel, Node node, JourneyState journeyState, int cost) {
        if (nodeLabel == TransportGraphBuilder.Labels.PLATFORM) {
            return toPlatform(node, journeyState, cost);
        }

        if (nodeLabel == TransportGraphBuilder.Labels.SERVICE) {
            return toService(node, cost);
        }
        throw new RuntimeException("Unexpected node type: "+nodeLabel);
    }

    private TraversalState toService(Node node, int cost) {
        Iterable<Relationship> relationships = node.getRelationships(OUTGOING, TO_HOUR);
        return new ServiceState(this, relationships, maybeExistingTrip, cost);
        //return new ServiceState(this, hourOrdered(relationships), maybeExistingTrip, cost);
    }

    private TraversalState toPlatform(Node node, JourneyState journeyState, int cost) {
        try {
            journeyState.leaveTram(getTotalCost());
            // if towards destination just return that one relationship
            for(Relationship relationship :  node.getRelationships(OUTGOING, LEAVE_PLATFORM)) {
                if (relationship.getProperty(GraphStaticKeys.STATION_ID).equals(destinationStationdId)) {
                    return new PlatformState(this, Collections.singleton(relationship), routeStationNodeId, cost);
                }
            }
            Iterable<Relationship> relationships = node.getRelationships(OUTGOING, BOARD, INTERCHANGE_BOARD, LEAVE_PLATFORM);

            return new PlatformState(this,
                    filterExcludingEndNode(relationships, routeStationNodeId), node.getId(), cost);
        }
        catch (TramchesterException exception) {
            throw new RuntimeException("Unable to process platform", exception);
        }
    }
}
