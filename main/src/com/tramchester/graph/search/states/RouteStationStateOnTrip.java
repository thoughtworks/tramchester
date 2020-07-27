package com.tramchester.graph.search.states;

import com.tramchester.domain.IdFor;
import com.tramchester.domain.TransportMode;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.input.Trip;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.search.JourneyState;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

import java.util.Collection;
import java.util.List;

import static com.tramchester.graph.TransportRelationshipTypes.LEAVE_PLATFORM;
import static java.lang.String.format;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class RouteStationStateOnTrip extends TraversalState implements NodeId {
    private final long routeStationNodeId;
    private final IdFor<Trip> tripId;

    public static class Builder {

        public TraversalState fromMinuteState(MinuteState minuteState, Node node, int cost, Collection<Relationship> routeStationOutbound,
                                              IdFor<Trip> tripId) {
            return new RouteStationStateOnTrip(minuteState, routeStationOutbound, cost, node.getId(), tripId);
        }
    }

    private RouteStationStateOnTrip(TraversalState parent, Iterable<Relationship> relationships, int cost,
                                    long routeStationNodeId, IdFor<Trip> tripId) {
        super(parent, relationships, cost);
        this.routeStationNodeId = routeStationNodeId;
        this.tripId = tripId;
    }

    @Override
    public String toString() {
        return "RouteStationStateOnTrip{" +
                "routeStationNodeId=" + routeStationNodeId +
                ", tripId='" + tripId + '\'' +
                "} " + super.toString();
    }

    @Override
    public TraversalState createNextState(Path path, GraphBuilder.Labels nodeLabel, Node nextNode,
                                          JourneyState journeyState, int cost) {
        switch (nodeLabel) {
            case PLATFORM:
                return toPlatform(nextNode, journeyState, cost);
            case SERVICE:
                return builders.service.fromRouteStation(this, tripId, nextNode, cost);
            case BUS_STATION:
            case TRAIN_STATION:
                return toStation(nextNode, journeyState, cost, nodeLabel);
            default:
                throw new RuntimeException(format("Unexpected node type: %s state :%s ", nodeLabel, this));
        }
    }

    private TraversalState toStation(Node stationNode, JourneyState journeyState, int cost, GraphBuilder.Labels label) {
        // no platforms in bus network, direct to station
        try {
            journeyState.leave(modeFromLabel(label), getTotalCost());
        } catch (TramchesterException e) {
            throw new RuntimeException("Unable to depart tram",e);
        }

        // if bus station then may have arrived
        long busStationNodeId = stationNode.getId();
        if (destinationNodeIds.contains(busStationNodeId)) {
            return builders.destination.from(this, cost);
        }

        return builders.noPlatformStation.fromRouteStation(this, stationNode, cost, label);
    }

    private TraversalState toPlatform(Node platformNode, JourneyState journeyState, int cost) {
        try {
            journeyState.leave(TransportMode.Tram, getTotalCost());

            // TODO Push into PlatformState
            List<Relationship> towardsDest = getTowardsDestination(platformNode.getRelationships(OUTGOING, LEAVE_PLATFORM));
            if (!towardsDest.isEmpty()) {
                return builders.platform.fromRouteStationTowardsDest(this, towardsDest, platformNode,  cost);
            }
            return builders.platform.fromRouteStationOnTrip(this, platformNode, cost);
        }
        catch (TramchesterException exception) {
            throw new RuntimeException("Unable to process platform", exception);
        }
    }

    @Override
    public long nodeId() {
        return routeStationNodeId;
    }

}
