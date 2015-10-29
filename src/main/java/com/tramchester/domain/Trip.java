package com.tramchester.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Trip {
    private static final Logger logger = LoggerFactory.getLogger(Trip.class);
    private final String serviceId;

    private String tripId;
    private String headSign;
    private Map<String, Stop> stops = new LinkedHashMap<>();

    public Trip(String tripId, String headSign, String serviceId) {
        this.tripId = tripId;
        this.headSign = headSign;
        this.serviceId = serviceId;
    }

    public String getTripId() {
        return tripId;
    }

    public List<Stop> getStops() {
        List<Stop> results = new LinkedList<>();
        results.addAll(stops.values());
        return results;
    }

    public void addStop(Stop stop) {
        Station station = stop.getStation();
        if (station==null) {
            logger.warn("Stop is missing station");
        } else {
            stops.put(station.getId(), stop);
        }
    }

    public boolean travelsBetween(String firstStationId, String lastStationId, int minutesFromMidnight) {
        boolean seenFirst = false;
        boolean seenSecond = false;
        for (Stop stop : stops.values()) {
            if (stop.getMinutesFromMidnight() > minutesFromMidnight) {
                String stopStationId = stop.getStation().getId();
                if (firstStationId.equals(stopStationId)) {
                    seenFirst = true;
                } else if (lastStationId.equals(stopStationId)) {
                    seenSecond = true;
                }
            }
            if (seenFirst && seenSecond) {
                return true;
            }
        }
        return false;
    }

    public Stop getStop(String stationId) {
        if (stops.containsKey(stationId)) {
            return stops.get(stationId);
        }
        logger.error(String.format("Could not find a stop in trip %s for station %s", tripId, stationId));
        return null;
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
}
