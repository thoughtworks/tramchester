package com.tramchester.domain.input;

import com.tramchester.domain.TimeWindow;
import com.tramchester.domain.TramTime;
import com.tramchester.domain.presentation.ServiceTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

import static java.lang.String.format;

public class Trip {
    private static final Logger logger = LoggerFactory.getLogger(Trip.class);

    private final String serviceId;
    private final String routeId;
    private final String tripId;
    private final String headSign;
    private final Stops stops;
    private TramTime earliestDepart = null;
    private TramTime latestDepart = null;

    public Trip(String tripId, String headSign, String serviceId, String routeId) {
        this.tripId = tripId.intern();
        this.headSign = headSign.intern();
        this.serviceId = serviceId.intern();
        this.routeId = routeId.intern();
        stops = new Stops();
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
        if (earliestDepart==null) {
            earliestDepart = departureTime;
        } else if (departureTime.compareTo(earliestDepart)<0) {
            earliestDepart = departureTime;
        }
        if (latestDepart==null) {
            latestDepart = departureTime;
        } else if (departureTime.compareTo(latestDepart)>0) {
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
                ", routeId='" + routeId + '\'' +
                ", tripId='" + tripId + '\'' +
                ", headSign='" + headSign + '\'' +
                ", stops=" + stops +
                '}';
    }

    public String getServiceId() {
        return serviceId;
    }

    public SortedSet<ServiceTime> getServiceTimes(String firstStationId, String lastStationId, TimeWindow window) {
        SortedSet<ServiceTime> serviceTimes = new TreeSet<>();
        for(BeginEnd pair : stops.getBeginEndStopsFor(firstStationId,lastStationId, window)) {
            ServiceTime serviceTime = new ServiceTime(pair.begin().getDepartureTime(), pair.end().getArrivalTime(),
                    serviceId, headSign, tripId);
            logger.info("Added " + serviceTime);
            serviceTimes.add(serviceTime);
        }
        return serviceTimes;
    }

    public boolean callsAt(String stationId) {
        return stops.callsAt(stationId);
    }

    public List<Stop> getStopsFor(String stationId) {
        return stops.getStopsFor(stationId);
    }

    public Optional<ServiceTime> earliestDepartFor(String firstStationId, String lastStationId, TimeWindow window) {
        int earliestMins = Integer.MAX_VALUE;
        Optional<ServiceTime> earliestTime = Optional.empty();

        for(BeginEnd pair : stops.getBeginEndStopsFor(firstStationId,lastStationId, window)) {
            Stop begin = pair.begin();
            int firstDepart = begin.getDepartureMinFromMidnight();
            if (firstDepart < earliestMins) {
                earliestMins = firstDepart;
                earliestTime = Optional.of(new ServiceTime(pair.begin().getDepartureTime(), pair.end().getArrivalTime(),
                        serviceId, headSign, tripId));
            }
        }

        if (!earliestTime.isPresent()) {
            logger.warn(format("Unable to find earlier time between %s and %s with %s", firstStationId,lastStationId,
                    window));
        }
        return earliestTime;
    }

    public String getHeadsign() {
        return headSign;
    }

    public String getRouteId() {
        return routeId;
    }

    public TramTime earliestDepartTime() {
        return earliestDepart;
    }

    public TramTime latestDepartTime() {
        return latestDepart;
    }
}
