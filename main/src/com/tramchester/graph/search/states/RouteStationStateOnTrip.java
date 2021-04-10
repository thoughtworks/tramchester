package com.tramchester.graph.search.states;

import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.input.Trip;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.search.JourneyState;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.tramchester.graph.TransportRelationshipTypes.LEAVE_PLATFORM;
import static java.lang.String.format;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class RouteStationStateOnTrip extends TraversalState implements NodeId {
    private final long routeStationNodeId;
    private final IdFor<Trip> tripId;
    private final TransportMode transportMode;

    public static class Builder {

        public TraversalState fromMinuteState(MinuteState minuteState, Node node, int cost, Collection<Relationship> routeStationOutbound,
                                              IdFor<Trip> tripId, TransportMode transportMode) {
            return new RouteStationStateOnTrip(minuteState, routeStationOutbound, cost, node.getId(), tripId, transportMode);
        }
    }

    private RouteStationStateOnTrip(TraversalState parent, Iterable<Relationship> relationships, int cost,
                                    long routeStationNodeId, IdFor<Trip> tripId, TransportMode transportMode) {
        super(parent, relationships, cost);
        this.routeStationNodeId = routeStationNodeId;
        this.tripId = tripId;
        this.transportMode = transportMode;
    }

    @Override
    public String toString() {
        return "RouteStationStateOnTrip{" +
                "routeStationNodeId=" + routeStationNodeId +
                ", tripId=" + tripId +
                ", transportMode=" + transportMode +
                "} " + super.toString();
    }

    @Override
    public TraversalState createNextState(Set<GraphBuilder.Labels> nodeLabels, Node nextNode,
                                          JourneyState journeyState, int cost) {
        // should be called for multi-mode stations only
        return toStation(nextNode, journeyState, cost);
    }

    @Override
    public TraversalState createNextState(GraphBuilder.Labels nodeLabel, Node nextNode,
                                          JourneyState journeyState, int cost) {
        return switch (nodeLabel) {
            case PLATFORM -> toPlatform(nextNode, journeyState, cost);
            case SERVICE -> builders.service.fromRouteStation(this, tripId, nextNode, cost);
            case BUS_STATION, TRAIN_STATION -> toStation(nextNode, journeyState, cost);
            default -> throw new RuntimeException(format("Unexpected node type: %s state :%s ", nodeLabel, this));
        };
    }

    private TraversalState toStation(Node stationNode, JourneyState journeyState, int cost) {
        // no platforms in bus network, direct to station
        try {
            journeyState.leave(transportMode, getTotalCost());
        } catch (TramchesterException e) {
            throw new RuntimeException("Unable to depart tram",e);
        }

        // if no platform station then may have arrived
        long stationNodeId = stationNode.getId();
        if (destinationNodeIds.contains(stationNodeId)) {
            return builders.destination.from(this, cost);
        }

        return builders.noPlatformStation.fromRouteStation(this, stationNode, cost);
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
