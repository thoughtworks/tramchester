package com.tramchester.graph.search.stateMachine.states;

import com.google.common.collect.Streams;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.InvalidId;
import com.tramchester.domain.input.Trip;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.ExistingTrip;
import com.tramchester.graph.search.stateMachine.RegistersFromState;
import com.tramchester.graph.search.stateMachine.Towards;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import static com.tramchester.graph.GraphPropertyKey.TRIP_ID;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class MinuteState extends TraversalState {

    public static class Builder implements Towards<MinuteState> {

        private final boolean changeAtInterchangeOnly;
        private final NodeContentsRepository nodeContents;

        public Builder(boolean changeAtInterchangeOnly, NodeContentsRepository nodeContents) {
            this.changeAtInterchangeOnly = changeAtInterchangeOnly;
            this.nodeContents = nodeContents;
        }

        @Override
        public void register(RegistersFromState registers) {
            registers.add(TraversalStateType.HourState, this);
        }

        @Override
        public TraversalStateType getDestination() {
            return TraversalStateType.MinuteState;
        }

        public TraversalState fromHour(HourState hourState, Node node, Duration cost, ExistingTrip existingTrip,
                                       JourneyStateUpdate journeyState, TransportRelationshipTypes[] currentModes) {

            //Iterable<Relationship> relationships = node.getRelationships(OUTGOING, TRAM_GOES_TO, BUS_GOES_TO, TRAIN_GOES_TO, FERRY_GOES_TO, SUBWAY_GOES_TO);
            Iterable<Relationship> relationships = node.getRelationships(OUTGOING, currentModes);

            if (existingTrip.isOnTrip()) {
                IdFor<Trip> existingTripId = existingTrip.getTripId();
                List<Relationship> filterBySingleTripId = filterBySingleTripId(relationships, existingTripId);
                return new MinuteState(hourState, filterBySingleTripId, existingTripId, cost, changeAtInterchangeOnly, this);
            } else {
                // starting a brand-new journey, since at minute node now have specific tripid to use
                IdFor<Trip> newTripId = getTrip(node);
                journeyState.beginTrip(newTripId);
                return new MinuteState(hourState, relationships, newTripId, cost, changeAtInterchangeOnly, this);
            }
        }

        private List<Relationship> filterBySingleTripId(Iterable<Relationship> relationships, IdFor<Trip> existingTripId) {
            return Streams.stream(relationships).
                    filter(relationship -> nodeContents.getTripId(relationship).equals(existingTripId)).
                    collect(Collectors.toList());

        }
    }

    private final boolean interchangesOnly;
    private final Trip trip;

    private MinuteState(TraversalState parent, Iterable<Relationship> relationships, IdFor<Trip> tripId, Duration cost,
                        boolean interchangesOnly, Towards<MinuteState> builder) {
        super(parent, relationships, cost, builder.getDestination());
        this.trip = traversalOps.getTrip(tripId);
        this.interchangesOnly = interchangesOnly;
    }

    private static IdFor<Trip> getTrip(Node endNode) {
        if (!GraphProps.hasProperty(TRIP_ID, endNode)) {
            return new InvalidId<>();
        }
        return GraphProps.getTripId(endNode);
    }

    public IdFor<Service> getServiceId() {
        return trip.getService().getId();
    }

    @Override
    protected RouteStationStateOnTrip toRouteStationOnTrip(RouteStationStateOnTrip.Builder towardsRouteStation,
                                                           Node routeStationNode, Duration cost, boolean isInterchange) {

        return towardsRouteStation.fromMinuteState(this, routeStationNode, cost, isInterchange, trip);
    }

    @Override
    protected RouteStationStateEndTrip toRouteStationEndTrip(RouteStationStateEndTrip.Builder towardsEndTrip,
                                                             Node routeStationNode,
                                                             Duration cost, boolean isInterchange) {

        return towardsEndTrip.fromMinuteState(this, routeStationNode, cost, isInterchange, trip);
    }

    @Override
    public String toString() {
        return "MinuteState{" +
                "interchangesOnly=" + interchangesOnly +
                ", trip='" + trip.getId() + '\'' +
                "} " + super.toString();
    }

}
