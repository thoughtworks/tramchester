package com.tramchester.domain;

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
    private Stops stops = new Stops(); // stationId -> Stop

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
        return stops.travelsBetween(firstStationId, lastStationId, minutesFromMidnight);
    }

    public String getHeadSign() {
        return headSign;
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
        for(Integer[] pair : stops.getPairs(firstStationId, lastStationId)) {
            Stop firstStop = stops.get(pair[0]);
            Stop secondStop = stops.get(pair[1]);
            int fromMidnight = firstStop.getDepartureMinFromMidnight();
            if (secondStop.getArriveMinsFromMidnight() > fromMidnight) {
                if (fromMidnight > minsFromMid) {
                    serviceTimes.add(new ServiceTime(firstStop.getDepartureTime(),
                            secondStop.getArrivalTime(), serviceId, headSign, fromMidnight));
                }
            }
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
        for(Integer[] pair : stops.getPairs(firstStationId, lastStationId)) {
            Stop firstStop = stops.get(pair[0]);
            Stop secondStop = stops.get(pair[1]);
            int firstDepart = firstStop.getDepartureMinFromMidnight();
            if (secondStop.getArriveMinsFromMidnight() > firstDepart) {
                if (firstDepart > minutesFromMidnight) {
                    if (firstDepart < earliest) {
                        earliest = firstDepart;
                    }
                }
            }
        }
        return earliest;
    }

}
