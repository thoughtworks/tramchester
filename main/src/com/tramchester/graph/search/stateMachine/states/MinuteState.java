package com.tramchester.graph.search.stateMachine.states;

import com.google.common.collect.Streams;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.InvalidId;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.graph.search.JourneyState;
import com.tramchester.graph.search.stateMachine.ExistingTrip;
import com.tramchester.graph.search.stateMachine.UnexpectedNodeTypeException;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.graph.GraphPropertyKey.TRIP_ID;
import static com.tramchester.graph.TransportRelationshipTypes.*;
import static com.tramchester.graph.graphbuild.GraphBuilder.Labels.INTERCHANGE;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class MinuteState extends TraversalState {

    private final boolean interchangesOnly;

    public static class Builder {

        private final TramchesterConfig config;

        public Builder(TramchesterConfig config) {
            this.config = config;
        }

        public TraversalState fromHour(HourState hourState, Node node, int cost, ExistingTrip maybeExistingTrip) {
            Iterable<Relationship> relationships = node.getRelationships(OUTGOING, TRAM_GOES_TO, BUS_GOES_TO, TRAIN_GOES_TO
                ,FERRY_GOES_TO, SUBWAY_GOES_TO);

            boolean changeAtInterchangeOnly = config.getChangeAtInterchangeOnly();
            if (maybeExistingTrip.isOnTrip()) {
                IdFor<Trip> existingTripId = maybeExistingTrip.getTripId();
                Iterable<Relationship> filterBySingleTripId =
                        hourState.traversalOps.filterBySingleTripId(relationships, existingTripId);
                return new MinuteState(hourState, filterBySingleTripId, existingTripId, cost, changeAtInterchangeOnly);
            } else {
                // starting a brand new journey
                IdFor<Trip> newTripId = getTrip(node);
                return new MinuteState(hourState, relationships, newTripId, cost, changeAtInterchangeOnly);
            }
        }
    }

    private final IdFor<Trip> tripId;

    private MinuteState(TraversalState parent, Iterable<Relationship> relationships, IdFor<Trip> tripId, int cost, boolean interchangesOnly) {
        super(parent, relationships, cost);
        this.tripId = tripId;
        this.interchangesOnly = interchangesOnly;
    }

    @Override
    public String toString() {
        return "MinuteState{" +
                "interchangesOnly=" + interchangesOnly +
                ", tripId='" + tripId + '\'' +
                "} " + super.toString();
    }

    private static IdFor<Trip> getTrip(Node endNode) {
        if (!GraphProps.hasProperty(TRIP_ID, endNode)) {
            return new InvalidId<>();
        }
        return GraphProps.getTripId(endNode);
    }

    @Override
    public TraversalState createNextState(GraphBuilder.Labels nodeLabel, Node node, JourneyState journeyState, int cost) {
        if (nodeLabel == GraphBuilder.Labels.ROUTE_STATION) {
            return toRouteStation(node, cost);
        }
        throw new UnexpectedNodeTypeException(node, "Unexpected node type: "+nodeLabel);
    }

    @Override
    public TraversalState createNextState(Set<GraphBuilder.Labels> nodeLabels, Node node, JourneyState journeyState, int cost) {
        // route station nodes may also have INTERCHANGE label set
        if (nodeLabels.contains(GraphBuilder.Labels.ROUTE_STATION)) {
            return toRouteStation(node, cost);
        }
        throw new UnexpectedNodeTypeException(node, "Unexpected node types: "+nodeLabels);
    }

    private TraversalState toRouteStation(Node routeStationNode, int cost) {
        Iterable<Relationship> allDeparts = routeStationNode.getRelationships(OUTGOING, DEPART, INTERCHANGE_DEPART);

        TransportMode transportMode = GraphProps.getTransportMode(routeStationNode);

        // if towards dest then always follow whether interchange-only enabled or not
        // towards final destination, just follow this one
        List<Relationship> towardsDestination = traversalOps.getTowardsDestination(allDeparts);
        if (!towardsDestination.isEmpty()) {
            // we've nearly arrived
            return builders.towardsRouteStateOnTrip(this).
                    fromMinuteState(this, routeStationNode, cost, towardsDestination, tripId, transportMode);
//            return builders.routeStationOnTrip.fromMinuteState(this, routeStationNode, cost, towardsDestination, tripId, transportMode);
        }

        // outbound service relationships that continue the current trip
        List<Relationship> routeStationOutbounds = filterByTripId(routeStationNode.getRelationships(OUTGOING, TO_SERVICE));
        boolean tripFinishedHere = routeStationOutbounds.isEmpty(); // i.e. no outbound from RS for this tripId

        // now add outgoing to platforms
        if (interchangesOnly) {
            if (routeStationNode.hasLabel(INTERCHANGE)) {
                Iterable<Relationship> interchanges = routeStationNode.getRelationships(OUTGOING, INTERCHANGE_DEPART);
                interchanges.forEach(routeStationOutbounds::add);
            }
        } else {
            allDeparts.forEach(routeStationOutbounds::add);
        }

        if (tripFinishedHere) {
            // for a change of trip id we need to get off vehicle, then back on to another service
            return builders.towardsRouteStateEndTrip(this).
                    fromMinuteState(this, cost, routeStationOutbounds, transportMode);
        } else {
            return builders.towardsRouteStateOnTrip(this).
                    fromMinuteState(this, routeStationNode, cost, routeStationOutbounds, tripId, transportMode);
        }
    }

    private List<Relationship> filterByTripId(Iterable<Relationship> svcRelationships) {

        IdFor<Service> currentSvcId = traversalOps.getServiceIdFor(tripId);
       //tripRepository.getTripById(tripId).getService().getId();

        return Streams.stream(svcRelationships).
                filter(relationship -> serviceNodeMatches(relationship, currentSvcId)).
                collect(Collectors.toList());
    }

    private boolean serviceNodeMatches(Relationship relationship, IdFor<Service> currentSvcId) {
        Node svcNode = relationship.getEndNode();
        IdFor<Service> svcId = traversalOps.getServiceIdFor(svcNode);
        return  currentSvcId.equals(svcId);
    }


}
