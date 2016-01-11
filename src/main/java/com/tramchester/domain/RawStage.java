package com.tramchester.domain;

public class RawStage {
    private final String firstStation;
    private final String mode;
    private final String routeName;
    private final String routeId;
    private String serviceId;
    private String lastStation;

    public RawStage(String firstStation, String routeName, String mode, String tramRouteId) {
        this.firstStation = firstStation;
        this.routeName = routeName;
        this.mode = mode;
        this.routeId = tramRouteId;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public void setLastStation(String lastStation) {
        this.lastStation = lastStation;
    }

    public String getFirstStation() {
        return firstStation;
    }

    public String getRouteName() {
        return routeName;
    }

    public String getMode() {
        return mode;
    }

    public String getRouteId() {
        return routeId;
    }

    public String getLastStation() {
        return lastStation;
    }
}
