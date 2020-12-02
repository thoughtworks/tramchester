package com.tramchester.domain.input;

import com.tramchester.domain.*;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.ServiceTime;
import com.tramchester.graph.GraphPropertyKey;

public class Trip implements HasId<Trip>, HasTransportMode, GraphProperty {

    private final IdFor<Trip> tripId;
    private final String headSign;
    private final Service service;
    private final Route route;
    private final StopCalls stops;
    private ServiceTime earliestDepart = null;
    private ServiceTime latestDepart = null;
    private int lastIndex;
    private int firstIndex;

    public Trip(String tripId, String headSign, Service service, Route route) {
        this(IdFor.createId(tripId), headSign, service, route);
    }

    public Trip(IdFor<Trip> tripId, String headSign, Service service, Route route) {
        this.tripId = tripId;
        this.headSign = headSign.intern();
        this.service = service;
        this.route = route;
        stops = new StopCalls();
        lastIndex = Integer.MIN_VALUE;
        firstIndex = Integer.MAX_VALUE;
    }

    // test memory support
    public void dispose() {
        stops.dispose();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Trip trip = (Trip) o;
        return tripId.equals(trip.tripId);
    }

    @Override
    public int hashCode() {
        return tripId != null ? tripId.hashCode() : 0;
    }

    public IdFor<Trip> getId() {
        return tripId;
    }

    public StopCalls getStops() {
        return stops;
    }

    public void addStop(StopCall stop) {
        stops.add(stop);

        // use stop index as avoids issues with crossing day boundaries
        int stopIndex = stop.getGetSequenceNumber();
        ServiceTime departureTime = stop.getDepartureTime();

        if (stopIndex < firstIndex) {
            firstIndex = stopIndex;
            earliestDepart = departureTime;
        }
        if (stopIndex > lastIndex) {
            lastIndex = stopIndex;
            latestDepart = departureTime;
        }
    }

    @Override
    public String toString() {
        return "Trip{" +
                "tripId='" + tripId + '\'' +
                ", headSign='" + headSign + '\'' +
                ", service=" + HasId.asId(service) +
                ", route=" + HasId.asId(route) +
                ", stops=" + stops +
                ", earliestDepart=" + earliestDepart +
                ", latestDepart=" + latestDepart +
                ", lastIndex=" + lastIndex +
                ", firstIndex=" + firstIndex +
                '}';
    }

    public Service getService() {
        return service;
    }

    public String getHeadsign() {
        return headSign;
    }

    public Route getRoute() {
        return route;
    }

    public ServiceTime earliestDepartTime() {
        if (earliestDepart==null) {
            throw new RuntimeException("earliestDepart not set for tripid " + tripId);
        }
        return earliestDepart;
    }

    public ServiceTime latestDepartTime() {
        if (latestDepart==null) {
            throw new RuntimeException("earliestDepart not set for tripid" + tripId);
        }
        return latestDepart;
    }

    @Override
    public TransportMode getTransportMode() {
        return route.getTransportMode();
    }

    public int getSeqNumOfFirstStop() {
        return firstIndex;
    }

    public int getSeqNumOfLastStop() {
        return lastIndex;
    }

    @Override
    public GraphPropertyKey getProp() {
        return GraphPropertyKey.TRIP_ID;
    }
}
