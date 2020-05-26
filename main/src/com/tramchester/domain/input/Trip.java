package com.tramchester.domain.input;

import com.tramchester.domain.HasId;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.time.TramTime;

import java.util.List;

public class Trip implements HasId {

    private final String tripId;
    private final String headSign;
    private final Service service;
    private final Route route;
    private final StopCalls stops;
    private TramTime earliestDepart = null;
    private TramTime latestDepart = null;
    private byte lastIndex;

    public Trip(String tripId, String headSign, Service service, Route route) {
        this.tripId = tripId.intern();
        this.headSign = headSign.intern();
        this.service = service;
        this.route = route;
        stops = new StopCalls();
        lastIndex = 0;
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
        TramTime departureTime = stop.getDepartureTime();
        // use stop index as avoids issues with crossing day boundaries
        byte stopIndex = stop.getGetSequenceNumber();
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

    public boolean callsAt(String stationId) {
        return stops.callsAt(stationId);
    }

    public List<StopCall> getStopsFor(String stationId) {
        return stops.getStopsFor(stationId);
    }

    public String getHeadsign() {
        return headSign;
    }

    public Route getRoute() {
        return route;
    }

    public TramTime earliestDepartTime() {
        return earliestDepart;
    }

    public TramTime latestDepartTime() {
        return latestDepart;
    }

    public boolean getTram() {
        return route.isTram();
    }
}
