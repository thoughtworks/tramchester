package com.tramchester.domain;

public class Stage {
    private String firstStation;
    private String route;
    private String lastStation;

    public Stage(String firstStation, String route) {
        this.firstStation = firstStation;
        this.route = route;
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
}
