package com.tramchester.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class Trip {
    private static final Logger logger = LoggerFactory.getLogger(Trip.class);
    private final String serviceId;

    private String tripId;
    private String headSign;
    private List<Stop> stops = new ArrayList<>();

    public Trip(String tripId, String headSign, String serviceId) {
        this.tripId = tripId;
        this.headSign = headSign;
        this.serviceId = serviceId;
    }

    public String getTripId() {
        return tripId;
    }

    public List<Stop> getStops() {
        return stops;
    }

    public void addStop(Stop stop) {
        stops.add(stop);
    }

//    public boolean isAfter(int minutesFromMidnight, String originStation) {
//        for (Stop stop : stops) {
//            String stopStationId = stop.getStation().getId();
//            if (stopStationId.equals(originStation) && stop.getMinutesFromMidnight() > minutesFromMidnight) {
//                return true;
//            }
//        }
//        return false;
//    }

    public boolean travelsBetween(String firstStationId, String lastStationId, int minutesFromMidnight) {
        boolean seenFirst = false;
        boolean seenSecond = false;
        for (Stop stop : stops) {
            if (stop.getMinutesFromMidnight()>=minutesFromMidnight) {
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
        for (Stop stop : stops) {
            if (stop.getStation().getId().equals(stationId)) {
                return stop;
            }
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
