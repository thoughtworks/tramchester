package com.tramchester.graph.search.states;

import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.search.JourneyState;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

import java.util.Collections;
import java.util.List;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class PlatformState extends TraversalState implements NodeId {

    public static class Builder {

        public PlatformState from(TramStationState tramStationState, Node node, int cost) {
            return new PlatformState(tramStationState,
                    node.getRelationships(OUTGOING, INTERCHANGE_BOARD, BOARD), node.getId(), cost);
        }

        public TraversalState fromRouteStationTowardsDest(RouteStationState routeStationState, Relationship relationship, Node platformNode, int cost) {
            return new PlatformState(routeStationState, Collections.singleton(relationship), platformNode.getId(), cost);
        }

        public TraversalState fromRouteStationOnTrip(RouteStationState routeStationState, Node node, int cost) {
            Iterable<Relationship> platformRelationships = node.getRelationships(OUTGOING,
                    BOARD, INTERCHANGE_BOARD, LEAVE_PLATFORM);
            // filter so we don't just get straight back on tram if just boarded, or if we are on an existing trip
            List<Relationship> filterExcludingEndNode = filterExcludingEndNode(platformRelationships, routeStationState);
            return new PlatformState(routeStationState, filterExcludingEndNode, node.getId(), cost);
        }

        public TraversalState fromRouteStation(RouteStationState routeStationState, Node node, int cost) {
            Iterable<Relationship> platformRelationships = node.getRelationships(OUTGOING,
                    BOARD, INTERCHANGE_BOARD, LEAVE_PLATFORM);
            // end of a trip, may need to go back to this route station to catch new service
            return new PlatformState(routeStationState, platformRelationships, node.getId(), cost);
        }
    }

    private final long platformNodeId;
    private final TramStationState.Builder tramStationStateBuilder;
    private final RouteStationStateJustBoarded.Builder routeStationStateBuilder;

    private PlatformState(TraversalState parent, Iterable<Relationship> relationships, long platformNodeId, int cost) {
        super(parent, relationships, cost);
        this.platformNodeId = platformNodeId;
        tramStationStateBuilder = new TramStationState.Builder();
        routeStationStateBuilder = new RouteStationStateJustBoarded.Builder(sortsPositions, destinationStationIds);
    }

    @Override
    public String toString() {
        return "PlatformState{" +
                "platformNodeId=" + platformNodeId +
                ", cost=" + super.getCurrentCost() +
                ", parent=" + parent +
                '}';
    }

    @Override
    public TraversalState createNextState(Path path, GraphBuilder.Labels nodeLabel, Node node, JourneyState journeyState, int cost) {

        long nodeId = node.getId();

        if (nodeLabel == GraphBuilder.Labels.TRAM_STATION) {
            if (nodeId == destinationNodeId) {
                return new DestinationState(this, cost);
            } else {
                return tramStationStateBuilder.fromPlatform(this, node, cost);
            }
        }

        if (nodeLabel == GraphBuilder.Labels.ROUTE_STATION) {
            try {
                journeyState.boardTram();
            } catch (TramchesterException e) {
                throw new RuntimeException("unable to board tram", e);
            }

            return routeStationStateBuilder.fromPlatformState(this, node, cost);

        }

        throw new RuntimeException("Unexpected node type: "+nodeLabel);
    }

    @Override
    public long nodeId() {
        return platformNodeId;
    }
}
