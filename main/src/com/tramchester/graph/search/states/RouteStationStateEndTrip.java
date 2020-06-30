package com.tramchester.graph.search.states;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.GraphStaticKeys;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.search.JourneyState;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

import java.util.List;

import static com.tramchester.graph.TransportRelationshipTypes.LEAVE_PLATFORM;
import static java.lang.String.format;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class RouteStationStateEndTrip extends TraversalState {

    private final boolean busesEnabled;

    @Override
    public String toString() {
        return "RouteStationStateEndTrip{" +
                "busesEnabled=" + busesEnabled +
                "} " + super.toString();
    }

    public static class Builder {
        private final TramchesterConfig config;

        public Builder(TramchesterConfig config) {
            this.config = config;
        }

        public TraversalState fromMinuteState(MinuteState minuteState, int cost, List<Relationship> routeStationOutbound) {
            return new RouteStationStateEndTrip(minuteState, routeStationOutbound, cost, config.getBus());
        }
    }


    private RouteStationStateEndTrip(MinuteState minuteState, List<Relationship> routeStationOutbound, int cost, boolean busesEnabled) {
        super(minuteState, routeStationOutbound, cost);
        this.busesEnabled = busesEnabled;
    }

    @Override
    public TraversalState createNextState(Path path, GraphBuilder.Labels nodeLabel, Node nextNode,
                                          JourneyState journeyState, int cost) {
        if (nodeLabel == GraphBuilder.Labels.PLATFORM) {
            return toPlatform(nextNode, journeyState, cost);
        }

        if (nodeLabel == GraphBuilder.Labels.SERVICE) {
            return builders.service.fromRouteStation(this, nextNode, cost);
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
        }
        catch (TramchesterException exception) {
            throw new RuntimeException("Unable to process platform", exception);
        }

        // TODO Push into PlatformState
        // if towards ONE destination just return that one relationship
        if (destinationStationIds.size()==1) {
            for (Relationship relationship : platformNode.getRelationships(OUTGOING, LEAVE_PLATFORM)) {
                if (destinationStationIds.contains(relationship.getProperty(GraphStaticKeys.STATION_ID).toString())) {
                    return builders.platform.fromRouteStationTowardsDest(this, relationship, platformNode,  cost);
                }
            }
        }

        return builders.platform.fromRouteStation(this, platformNode, cost);

    }
}
