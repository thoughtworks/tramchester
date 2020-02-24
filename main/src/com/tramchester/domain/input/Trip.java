package com.tramchester.domain.input;

import com.tramchester.domain.Route;
import com.tramchester.domain.time.TimeWindow;
import com.tramchester.domain.time.TramTime;

import java.util.List;

public class Trip {

    private final String serviceId;
    private final Route route;
    private final String tripId;
    private final String headSign;
    private final Stops stops;
    private TramTime earliestDepart = null;
    private TramTime latestDepart = null;
    private int lastIndex;

    public Trip(String tripId, String headSign, String serviceId, Route route) {
        this.tripId = tripId.intern();
        this.headSign = headSign.intern();
        this.serviceId = serviceId.intern();
        this.route = route;
        stops = new Stops();
        lastIndex = 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Trip trip = (Trip) o;

        return !(tripId != null ? !tripId.equals(trip.tripId) : trip.tripId != null);
    }

    @Override
    public int hashCode() {
        return tripId != null ? tripId.hashCode() : 0;
    }

    public String getTripId() {
        return tripId;
    }

    public Stops getStops() {
        return stops;
    }

    public void addStop(Stop stop) {
        stops.add(stop);
        TramTime departureTime = stop.getDepartureTime();
        int stopIndex = stop.getGetSequenceNumber();
        if (stopIndex ==1) {
            earliestDepart = departureTime;
        }
        if (stopIndex > lastIndex) {
            lastIndex = stopIndex;
            latestDepart = departureTime;
        }
    }

    public boolean travelsBetween(String firstStationId, String lastStationId, TimeWindow window) {
        return stops.travelsBetween(firstStationId, lastStationId, window);
    }

    @Override
    public String toString() {
        return "Trip{" +
                "serviceId='" + serviceId + '\'' +
                ", route='" + route + '\'' +
                ", tripId='" + tripId + '\'' +
                ", headSign='" + headSign + '\'' +
                ", stops=" + stops +
                '}';
    }

    public String getServiceId() {
        return serviceId;
    }

    public boolean callsAt(String stationId) {
        return stops.callsAt(stationId);
    }

    public List<Stop> getStopsFor(String stationId) {
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
}
