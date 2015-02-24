package com.tramchester.domain;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.mappers.TimeJsonSerializer;
import org.joda.time.DateTime;

import java.util.List;

public class Stage {
    private String firstStation;
    private String route;
    private String routeId;
    private String lastStation;
    private String serviceId;
    private List<ServiceTime> serviceTimes;

    public Stage(String firstStation, String route, String routeId) {
        this.firstStation = firstStation;
        this.route = route;
        this.routeId = routeId;
    }

    public void setLastStation(String lastStation) {
        this.lastStation = lastStation;
    }

    public String getFirstStation() {
        return firstStation;
    }

    public String getRoute() {
        return route;
    }

    public String getLastStation() {
        return lastStation;
    }

    public String getRouteId() {
        return routeId;
    }

    public String getTramRouteId() {
        return routeId.substring(4, 8);
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceTimes(List<ServiceTime> times) {
        this.serviceTimes = times;
    }

    public List<ServiceTime> getServiceTimes() {
        return serviceTimes;
    }

    @JsonSerialize(using = TimeJsonSerializer.class)
    public DateTime getExpectedArrivalTime() {
        DateTime firstArrivalTime = DateTime.now();
        if (serviceTimes.size() > 0) {
            firstArrivalTime = serviceTimes.get(0).getArrivalTime();
        }
        return firstArrivalTime;
    }
    @JsonSerialize(using = TimeJsonSerializer.class)
    public DateTime getFirstDepartureTime() {
        DateTime firstDepartureTime = DateTime.now();
        if (serviceTimes.size() > 0) {
            firstDepartureTime = serviceTimes.get(0).getDepartureTime();
        }
        return firstDepartureTime;
    }
}
