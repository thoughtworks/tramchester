package com.tramchester.domain;

import com.tramchester.domain.presentation.ServiceTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

import static java.lang.String.format;

public class Trip {
    private static final Logger logger = LoggerFactory.getLogger(Trip.class);
    private final String serviceId;
    private String tripId;
    private String headSign;
    private Stops stops = new Stops();

    public Trip(String tripId, String headSign, String serviceId) {
        this.tripId = tripId;
        this.headSign = headSign;
        this.serviceId = serviceId;
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
    }

    public boolean travelsBetween(String firstStationId, String lastStationId, int minutesFromMidnight) {
        logger.debug(format("check if stops for trip %s cover %s to %s at %s", tripId, firstStationId, lastStationId,
                minutesFromMidnight));
        return stops.travelsBetween(firstStationId, lastStationId, minutesFromMidnight);
    }

    @Override
    public String toString() {
        return "Trip{" +
                "tripId='" + tripId + '\'' +
                ", headSign='" + headSign + '\'' +
                ", service='" + serviceId + '\'' +
                ", stops=" + stops +
                '}';
    }

    public String getServiceId() {
        return serviceId;
    }

    public List<ServiceTime> getServiceTimes(String firstStationId, String lastStationId, int minsFromMid) {
        logger.info(format("Find service times between %s and %s at %s", firstStationId,lastStationId,minsFromMid));
        List<ServiceTime> serviceTimes = new LinkedList<>();
        for(Stop[] pair : stops.getBeginEndStopsFor(firstStationId,lastStationId, minsFromMid)) {
            Stop firstStop = pair[0];
            Stop secondStop = pair[1];

            int actualDepartTime = firstStop.getDepartureMinFromMidnight();

            serviceTimes.add(new ServiceTime(firstStop.getDepartureTime(), secondStop.getArrivalTime(),
                    serviceId, headSign, actualDepartTime));
        }
        return serviceTimes;
    }

    public boolean callsAt(String stationId) {
        return stops.callsAt(stationId);
    }

    public List<Stop> getStopsFor(String stationId) {
        return stops.getStopsFor(stationId);
    }

    public int earliestDepartFor(String firstStationId, String lastStationId, int minutesFromMidnight) {
        logger.info(format("Find first earliest depart time between %s and %s at %s for trip %s",
                firstStationId,lastStationId,minutesFromMidnight, tripId));
        int earliest = Integer.MAX_VALUE;
        for(Stop[] pair : stops.getBeginEndStopsFor(firstStationId,lastStationId, minutesFromMidnight)) {
            int firstDepart = pair[0].getDepartureMinFromMidnight();
            if (firstDepart < earliest) {
                earliest = firstDepart;
            }
        }
        return earliest;
    }

}
