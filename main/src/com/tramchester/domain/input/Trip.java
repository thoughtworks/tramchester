package com.tramchester.domain.input;

import com.tramchester.domain.*;
import com.tramchester.domain.time.ServiceTime;

public class Trip implements HasId, HasTransportMode {

    private final String tripId;
    private final String headSign;
    private final Service service;
    private final Route route;
    private final StopCalls stops;
    private ServiceTime earliestDepart = null;
    private ServiceTime latestDepart = null;
    private int lastIndex;

    public Trip(String tripId, String headSign, Service service, Route route) {
        this.tripId = tripId.intern();
        this.headSign = headSign.intern();
        this.service = service;
        this.route = route;
        stops = new StopCalls();
        lastIndex = 0;
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

    public String getId() {
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

        if (stopIndex == 1) {
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
                "serviceId='" + service.getId() + '\'' +
                ", route='" + route + '\'' +
                ", tripId='" + tripId + '\'' +
                ", headSign='" + headSign + '\'' +
                ", stops=" + stops +
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
            throw new RuntimeException("earliestDepart not set for " + tripId);
        }
        return earliestDepart;
    }

    public ServiceTime latestDepartTime() {
        if (latestDepart==null) {
            throw new RuntimeException("earliestDepart not set for " + tripId);
        }
        return latestDepart;
    }

//    public boolean getTram() {
//        return route.isTram();
//    }

    @Override
    public TransportMode getTransportMode() {
        return route.getTransportMode();
    }
}
