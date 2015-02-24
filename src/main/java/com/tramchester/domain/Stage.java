package com.tramchester.domain;

import java.util.List;

public class Stage {
    private String firstStation;
    private String route;
    private String routeId;
    private String lastStation;
    private String serviceId;
    private List<ServiceTime> times;

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

    public void setTimes(List<ServiceTime> times) {
        this.times = times;
    }

    public List<ServiceTime> getTimes() {
        return times;
    }
}
