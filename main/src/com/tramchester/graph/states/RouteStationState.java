package com.tramchester.graph.states;

import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.GraphStaticKeys;
import com.tramchester.graph.JourneyState;
import com.tramchester.graph.TransportGraphBuilder;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class RouteStationState extends TraversalState {
    private final long routeStationNodeId;
    private final boolean justBoarded;
    private Optional<String> maybeExistingTrip;

    public RouteStationState(TraversalState parent, Iterable<Relationship> relationships, long routeStationNodeId,
                             int cost, boolean justBoarded) {
        super(parent, relationships, cost);
        this.routeStationNodeId = routeStationNodeId;
        this.justBoarded = justBoarded;
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
                ", cost=" + super.getCurrentCost() +
                ", justBoarded=" + justBoarded +
                ", maybeExistingTrip='" + maybeExistingTrip + '\'' +
                ", parent=" + parent +
                '}';
    }

    @Override
    public TraversalState nextState(Path path, TransportGraphBuilder.Labels nodeLabel, Node nextNode,
                                    JourneyState journeyState, int cost) {
        if (nodeLabel == TransportGraphBuilder.Labels.PLATFORM) {
            return toPlatform(nextNode, journeyState, cost);
        }

        if (nodeLabel == TransportGraphBuilder.Labels.SERVICE) {
            return toService(nextNode, cost);
        }
        throw new RuntimeException("Unexpected node type: "+nodeLabel);
    }

    private TraversalState toService(Node serviceNode, int cost) {
        Iterable<Relationship> serviceRelationships = serviceNode.getRelationships(OUTGOING, TO_HOUR);
        return new ServiceState(this, serviceRelationships, maybeExistingTrip, cost);
    }

    private TraversalState toPlatform(Node platformNode, JourneyState journeyState, int cost) {
        try {
            journeyState.leaveTram(getTotalCost());

            // if towards ONE destination just return that one relationship
            if (destinationStationdIds.size()==1) {
                for (Relationship relationship : platformNode.getRelationships(OUTGOING, LEAVE_PLATFORM)) {
                    if (destinationStationdIds.contains(relationship.getProperty(GraphStaticKeys.STATION_ID).toString())) {
                        return new PlatformState(this, Collections.singleton(relationship), routeStationNodeId, cost);
                    }
                }
            }
            Iterable<Relationship> platformRelationships = platformNode.getRelationships(OUTGOING,
                    BOARD, INTERCHANGE_BOARD, LEAVE_PLATFORM);

            if (maybeExistingTrip.isPresent() || justBoarded) {
                // filter so we don't just get straight back on tram if just boarded, or if we are on an existing trip
                List<Relationship> filterExcludingEndNode = filterExcludingEndNode(platformRelationships, routeStationNodeId);
                return new PlatformState(this, filterExcludingEndNode, platformNode.getId(), cost);
            } else {
                // end of a trip, may need to go back to this route station to catch new service
                return new PlatformState(this, platformRelationships, platformNode.getId(), cost);
            }
        }
        catch (TramchesterException exception) {
            throw new RuntimeException("Unable to process platform", exception);
        }
    }
}
