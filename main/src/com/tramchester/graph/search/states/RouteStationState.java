package com.tramchester.graph.search.states;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.GraphStaticKeys;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.search.JourneyState;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

import java.util.Collection;

import static com.tramchester.graph.TransportRelationshipTypes.LEAVE_PLATFORM;
import static java.lang.String.format;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class RouteStationState extends TraversalState implements NodeId {
    private final long routeStationNodeId;
    private final String tripId;
    private final boolean busesEnabled;

    public static class Builder {

        private final TramchesterConfig config;

        public Builder(TramchesterConfig config) {
            this.config = config;
        }

        public TraversalState fromMinuteState(MinuteState minuteState, Node node, int cost, Collection<Relationship> routeStationOutbound,
                                              String tripId) {
            return new RouteStationState(minuteState, routeStationOutbound, cost, node.getId(), tripId, config.getBus());
        }
    }

    private RouteStationState(TraversalState parent, Iterable<Relationship> relationships, int cost,
                              long routeStationNodeId, String tripId, boolean busesEnabled) {
        super(parent, relationships, cost);
        this.routeStationNodeId = routeStationNodeId;
        this.tripId = tripId;
        this.busesEnabled = busesEnabled;
    }

    @Override
    public String toString() {
        return "RouteStationState{" +
                "routeStationNodeId=" + routeStationNodeId +
                ", tripId='" + tripId + '\'' +
                ", busesEnabled=" + busesEnabled +
                "} " + super.toString();
    }

    @Override
    public TraversalState createNextState(Path path, GraphBuilder.Labels nodeLabel, Node nextNode,
                                          JourneyState journeyState, int cost) {
        if (nodeLabel == GraphBuilder.Labels.PLATFORM) {
            return toPlatform(nextNode, journeyState, cost);
        }

        if (nodeLabel == GraphBuilder.Labels.SERVICE) {
            return builders.service.fromRouteStation(this, tripId, nextNode, cost);
        }
        if (busesEnabled && (nodeLabel == GraphBuilder.Labels.BUS_STATION)) {
            return toBusStation(nextNode, journeyState, cost);
        }

        throw new RuntimeException(format("Unexpected node type: %s state :%s ", nodeLabel, this));
    }

    private TraversalState toBusStation(Node busStationNode, JourneyState journeyState, int cost) {
        // no platforms in bus network, direct to station
        try {
            journeyState.leaveBus(getTotalCost());
        } catch (TramchesterException e) {
            throw new RuntimeException("Unable to depart tram",e);
        }

        // if bus station then may have arrived
        long busStationNodeId = busStationNode.getId();
        if (busStationNodeId == destinationNodeId) {
            return new DestinationState(this, cost);
        }

        return builders.busStation.fromRouteStation(this, busStationNode, cost);
    }

    private TraversalState toPlatform(Node platformNode, JourneyState journeyState, int cost) {
        try {
            journeyState.leaveTram(getTotalCost());

            // TODO Push into PlatformState
            // if towards ONE destination just return that one relationship
            if (destinationStationIds.size()==1) {
                for (Relationship relationship : platformNode.getRelationships(OUTGOING, LEAVE_PLATFORM)) {
                    if (destinationStationIds.contains(relationship.getProperty(GraphStaticKeys.STATION_ID).toString())) {
                        return builders.platform.fromRouteStationTowardsDest(this, relationship, platformNode,  cost);
                    }
                }
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
