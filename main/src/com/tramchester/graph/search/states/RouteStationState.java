package com.tramchester.graph.search.states;

import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.geo.SortsPositions;
import com.tramchester.graph.GraphStaticKeys;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.search.JourneyState;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

import java.util.*;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static java.lang.String.format;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class RouteStationState extends TraversalState implements NodeId {
    private final long routeStationNodeId;
    private final boolean justBoarded;
    private final ExistingTrip maybeExistingTrip;
    private final BusStationState.Builder busStationStateBuilder;
    private final ServiceState.Builder serviceStateBuilder;
    private final PlatformState.Builder platformStateBuilder;

    public static class Builder {
        private final SortsPositions sortsPositions;
        private final List<String> destinationStationIds;

        public Builder(SortsPositions sortsPositions, List<String> destinationStationIds) {
            this.sortsPositions = sortsPositions;
            this.destinationStationIds = destinationStationIds;
        }

        public RouteStationState fromPlatformState(PlatformState platformState, Node node, int cost) {
            List<Relationship> outbounds = filterExcludingEndNode(node.getRelationships(OUTGOING, ENTER_PLATFORM), platformState);
            node.getRelationships(OUTGOING, TO_SERVICE).forEach(outbounds::add);
            return new RouteStationState(platformState, outbounds, cost, node.getId(), ExistingTrip.none(), true);
        }

        public TraversalState fromBusStation(BusStationState busStationState, Node node, int cost) {
            List<Relationship> outbounds = filterExcludingEndNode(node.getRelationships(OUTGOING,
                    DEPART, INTERCHANGE_DEPART), busStationState);
            outbounds.addAll(orderSvcRelationships(node));
            return new RouteStationState(busStationState, outbounds, cost, node.getId(), ExistingTrip.none(), true);
        }

        public TraversalState fromMinuteState(MinuteState minuteState, Node node, int cost, Collection<Relationship> routeStationOutbound,
                                              ExistingTrip existingTrip) {
            return new RouteStationState(minuteState, routeStationOutbound, cost, node.getId(), existingTrip, false);
        }

        private Collection<Relationship> orderSvcRelationships(Node node) {
            Iterable<Relationship> toServices = node.getRelationships(OUTGOING, TO_SERVICE);

            List<SortsPositions.HasStationId<Relationship>> relationships = new ArrayList<>();
            toServices.forEach(svcRelationship -> relationships.add(new RelationshipFacade(svcRelationship)));

            return sortsPositions.sortedByNearTo(destinationStationIds, relationships);
        }

    }

    private RouteStationState(TraversalState parent, Iterable<Relationship> relationships, int cost,
                              long routeStationNodeId, ExistingTrip existingTrip, boolean justBoarded) {
        super(parent, relationships, cost);
        this.routeStationNodeId = routeStationNodeId;
        this.justBoarded = justBoarded;
        this.maybeExistingTrip = existingTrip;
        busStationStateBuilder = new BusStationState.Builder();
        serviceStateBuilder = new ServiceState.Builder();
        platformStateBuilder = new PlatformState.Builder();
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
            return serviceStateBuilder.fromRouteStation(this, maybeExistingTrip, nextNode, cost);

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

        if (maybeExistingTrip.isOnTrip() || justBoarded) {
            return busStationStateBuilder.fromRouteStationOnTrip(this, busStationNode, cost);
        } else {
            return busStationStateBuilder.fromRouteStation(this, busStationNode, cost);
        }
    }

    private TraversalState toPlatform(Node platformNode, JourneyState journeyState, int cost) {
        try {
            journeyState.leaveTram(getTotalCost());

            // TODO Push into PlatformState
            // if towards ONE destination just return that one relationship
            if (destinationStationIds.size()==1) {
                for (Relationship relationship : platformNode.getRelationships(OUTGOING, LEAVE_PLATFORM)) {
                    if (destinationStationIds.contains(relationship.getProperty(GraphStaticKeys.STATION_ID).toString())) {
                        return platformStateBuilder.fromRouteStationTowardsDest(this, relationship, platformNode,  cost);
                    }
                }
            }

            if (maybeExistingTrip.isOnTrip() || justBoarded) {
                return platformStateBuilder.fromRouteStationOnTrip(this, platformNode, cost);

            } else {
                return platformStateBuilder.fromRouteStation(this, platformNode, cost);

            }
        }
        catch (TramchesterException exception) {
            throw new RuntimeException("Unable to process platform", exception);
        }
    }

    @Override
    public long nodeId() {
        return routeStationNodeId;
    }


    private static class RelationshipFacade implements SortsPositions.HasStationId<Relationship> {
        private final Relationship relationship;

        private RelationshipFacade(Relationship relationship) {
            this.relationship = relationship;
        }

        @Override
        public String getStationId() {
            return relationship.getProperty(GraphStaticKeys.TOWARDS_STATION_ID).toString();
        }

        @Override
        public Relationship getContained() {
            return relationship;
        }
    }
}
