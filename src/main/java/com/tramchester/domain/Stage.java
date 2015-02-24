package com.tramchester.domain;

public class Stage {
    private String firstStation;
    private String route;
    private String routeId;
    private String lastStation;

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
}
