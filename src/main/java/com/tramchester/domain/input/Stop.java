package com.tramchester.domain.input;

import com.tramchester.domain.Location;
import com.tramchester.domain.TimeAsMinutes;
import com.tramchester.domain.TramTime;
import org.joda.time.LocalTime;

public class Stop extends TimeAsMinutes {
    private final Location station;
    private final TramTime arrivalTime;
    private final TramTime departureTime;
    private final String stopId;
    private final String routeId;
    private final String serviceId;

    public Stop(String stopId, Location station, LocalTime arrivalTime, LocalTime departureTime, String routeId, String serviceId) {

        this.stopId = stopId.intern();
        this.arrivalTime = TramTime.create(arrivalTime);
        this.departureTime = TramTime.create(departureTime);
        this.station = station;
        this.routeId = routeId.intern();
        this.serviceId = serviceId.intern();
    }

    public TramTime getArrivalTime() {
        return arrivalTime;
    }

    public TramTime getDepartureTime() {
        return departureTime;
    }

    public Location getStation() {
        return station;
    }

    @Override
    public String toString() {
        return "Stop{" +
                "station=" + station +
                ", arrivalTime=" + arrivalTime +
                ", departureTime=" + departureTime +
                ", stopId='" + stopId + '\'' +
                ", serviceId='" + serviceId + '\'' +
                ", routeId='" + routeId + '\'' +
                '}';
    }

    public int getDepartureMinFromMidnight() {
        return getMinutes(departureTime);
    }

    public int getArriveMinsFromMidnight() {
        return getMinutes(arrivalTime);
    }

    public String getId() {
        return stopId;
    }
}
