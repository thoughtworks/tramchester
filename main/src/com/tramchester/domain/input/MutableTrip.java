package com.tramchester.domain.input;

import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphPropertyKey;

public class MutableTrip implements Trip {

    private final IdFor<Trip> tripId;
    private final String headSign;
    private final Service service;
    private final Route route;
    private final StopCalls stopCalls;
    private final TransportMode actualMode; // used for things like RailReplacementBus where parent route has different mode
    private boolean filtered; // at least one station on this trip was filtered out

    public MutableTrip(IdFor<Trip> tripId, String headSign, Service service, Route route) {
        this(tripId, headSign, service, route, route.getTransportMode());
    }

    public MutableTrip(IdFor<Trip> tripId, String headSign, Service service, Route route, TransportMode actualMode) {
        this.tripId = tripId;
        this.headSign = headSign.intern();
        this.service = service;
        this.route = route;
        stopCalls = new StopCalls(tripId);
        this.actualMode = actualMode;
        filtered = false;
    }

    // test support
    public static Trip build(IdFor<Trip> tripId, String headSign, Service service, Route route) {
        return new MutableTrip(tripId, headSign, service, route);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MutableTrip trip = (MutableTrip) o;
        return tripId.equals(trip.tripId);
    }

    @Override
    public int hashCode() {
        return tripId != null ? tripId.hashCode() : 0;
    }

    @Override
    public IdFor<Trip> getId() {
        return tripId;
    }

    @Override
    public StopCalls getStopCalls() {
        return stopCalls;
    }

    public void addStop(StopCall stop) {
        stopCalls.add(stop);
    }

    @Override
    public String toString() {
        return "Trip{" +
                "tripId='" + tripId + '\'' +
                ", headSign='" + headSign + '\'' +
                ", service=" + HasId.asId(service) +
                ", route=" + HasId.asId(route) +
                ", stops=" + stopCalls +
                '}';
    }

    @Override
    public Service getService() {
        return service;
    }

    @Override
    public String getHeadsign() {
        return headSign;
    }

    @Override
    public Route getRoute() {
        return route;
    }

    @Override
    public TransportMode getTransportMode() {
        return actualMode;
    }

    @Override
    public GraphPropertyKey getProp() {
        return GraphPropertyKey.TRIP_ID;
    }

    public void setFiltered(boolean flag) {
        this.filtered = flag;
    }

    @Override
    public boolean isFiltered() {
        return filtered;
    }

    @Override
    public boolean intoNextDay() {
        return stopCalls.intoNextDay();
    }

    @Override
    public TramTime departTime() {
        return stopCalls.getFirstStop().getDepartureTime();
    }

    @Override
    public TramTime arrivalTime() {
        return stopCalls.getLastStop().getArrivalTime();
    }
}
