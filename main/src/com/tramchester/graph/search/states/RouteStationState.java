package com.tramchester.graph.search.states;

import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.GraphStaticKeys;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.search.JourneyState;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

import java.util.Collections;
import java.util.List;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static java.lang.String.format;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class RouteStationState extends TraversalState {
    private final long routeStationNodeId;
    private final boolean justBoarded;
    private final ExistingTrip maybeExistingTrip;

    public RouteStationState(TraversalState parent, Iterable<Relationship> relationships, long routeStationNodeId,
                             int cost, boolean justBoarded) {
        super(parent, relationships, cost);
        this.routeStationNodeId = routeStationNodeId;
        this.justBoarded = justBoarded;
        maybeExistingTrip = ExistingTrip.none();
    }

    public RouteStationState(TraversalState parent, Iterable<Relationship> relationships,
                             long routeStationNodeId, String tripId, int cost) {
        super(parent, relationships, cost);
        this.routeStationNodeId = routeStationNodeId;
        this.justBoarded = false;
        maybeExistingTrip = ExistingTrip.onTrip(tripId);
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
    public TraversalState createNextState(Path path, GraphBuilder.Labels nodeLabel, Node nextNode,
                                          JourneyState journeyState, int cost) {
        if (nodeLabel == GraphBuilder.Labels.PLATFORM) {
            return toPlatform(nextNode, journeyState, cost);
        }

        if (nodeLabel == GraphBuilder.Labels.SERVICE) {
            return toService(nextNode, cost);
        }
        if (config.getBus() && (nodeLabel == GraphBuilder.Labels.BUS_STATION)) {
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

        List<Relationship> stationRelationships = filterExcludingEndNode(busStationNode.getRelationships(OUTGOING, BOARD,
                INTERCHANGE_BOARD, WALKS_FROM), routeStationNodeId);
        if (maybeExistingTrip.isOnTrip() || justBoarded) {
            // filter so we don't just get straight back on tram if just boarded, or if we are on an existing trip
            List<Relationship> filterExcludingEndNode = filterExcludingEndNode(stationRelationships, routeStationNodeId);
            //return new PlatformState(this, filterExcludingEndNode, platformNode.getId(), cost);
            return new BusStationState(this, filterExcludingEndNode, cost, busStationNodeId);
        } else {
            // end of a trip, may need to go back to this route station to catch new service
            return new BusStationState(this, stationRelationships, cost, busStationNodeId);
        }
    }

    private TraversalState toService(Node serviceNode, int cost) {
        Iterable<Relationship> serviceRelationships = serviceNode.getRelationships(OUTGOING, TO_HOUR);
        return new ServiceState(this, serviceRelationships, maybeExistingTrip, cost);
    }

    private TraversalState toPlatform(Node platformNode, JourneyState journeyState, int cost) {
        try {
            journeyState.leaveTram(getTotalCost());

            // if towards ONE destination just return that one relationship
            if (destinationStationIds.size()==1) {
                for (Relationship relationship : platformNode.getRelationships(OUTGOING, LEAVE_PLATFORM)) {
                    if (destinationStationIds.contains(relationship.getProperty(GraphStaticKeys.STATION_ID).toString())) {
                        return new PlatformState(this, Collections.singleton(relationship), routeStationNodeId, cost);
                    }
                }
            }
            Iterable<Relationship> platformRelationships = platformNode.getRelationships(OUTGOING,
                    BOARD, INTERCHANGE_BOARD, LEAVE_PLATFORM);

            if (maybeExistingTrip.isOnTrip() || justBoarded) {
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
