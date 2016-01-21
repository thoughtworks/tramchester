package com.tramchester.domain;

public class RawStage {
    private final String firstStation;
    private final String mode;
    private final String routeName;
    private final String routeId;
    private int elapsedTime;
    private String serviceId;
    private String lastStation;

    public RawStage(String firstStation, String routeName, String mode, String tramRouteDisplayClass, int elapsedTime) {
        this.firstStation = firstStation;
        this.routeName = routeName;
        this.mode = mode;
        this.routeId = tramRouteDisplayClass;
        this.elapsedTime = elapsedTime;
    }

    public String getServiceId() {
        return serviceId;
    }

    public RawStage setServiceId(String serviceId) {
        this.serviceId = serviceId;
        return this;
    }

    public RawStage setLastStation(String lastStation) {
        this.lastStation = lastStation;
        return this;
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

    @Override
    public String toString() {
        return "RawStage{" +
                "firstStation='" + firstStation + '\'' +
                ", mode='" + mode + '\'' +
                ", routeName='" + routeName + '\'' +
                ", routeId='" + routeId + '\'' +
                ", serviceId='" + serviceId + '\'' +
                ", lastStation='" + lastStation + '\'' +
                '}';
    }

    public int getElapsedTime() {
        return elapsedTime;
    }
}
