package com.tramchester.domain.input;

import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphPropertyKey;

public class MutableTrip implements Trip {

    private final IdFor<Trip> tripId;
    private final String headSign;
    private final Service service;
    private final Route route;
    private final StopCalls stopCalls;
    private TramTime earliestDepart = null;
    private TramTime latestDepart = null;
    private int lastIndex;
    private int firstIndex;
    private boolean filtered; // at least one station on this trip was filtered out

    @Deprecated
    public MutableTrip(String tripId, String headSign, Service service, Route route) {
        this(StringIdFor.createId(tripId), headSign, service, route);
    }

    public MutableTrip(IdFor<Trip> tripId, String headSign, Service service, Route route) {
        this.tripId = tripId;
        this.headSign = headSign.intern();
        this.service = service;
        this.route = route;
        stopCalls = new StopCalls(tripId);
        lastIndex = Integer.MIN_VALUE;
        firstIndex = Integer.MAX_VALUE;
        filtered = false;
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

        // use stop index as avoids issues with crossing day boundaries
        int stopIndex = stop.getGetSequenceNumber();
        TramTime departureTime = stop.getDepartureTime();

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
                ", stops=" + stopCalls +
                ", earliestDepart=" + earliestDepart +
                ", latestDepart=" + latestDepart +
                ", lastIndex=" + lastIndex +
                ", firstIndex=" + firstIndex +
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
    public TramTime earliestDepartTime() {
        if (earliestDepart==null) {
            throw new RuntimeException("earliestDepart not set for tripid " + tripId);
        }
        return earliestDepart;
    }

    @Override
    public TramTime latestDepartTime() {
        if (latestDepart==null) {
            throw new RuntimeException("earliestDepart not set for tripid" + tripId);
        }
        return latestDepart;
    }

    @Override
    public TransportMode getTransportMode() {
        return route.getTransportMode();
    }

    @Override
    public int getSeqNumOfFirstStop() {
        return firstIndex;
    }

    @Override
    public int getSeqNumOfLastStop() {
        return lastIndex;
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
}
