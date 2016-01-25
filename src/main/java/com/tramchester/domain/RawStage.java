package com.tramchester.domain;

public class RawStage {
    private final String firstStation;
    private final TransportMode mode;
    private final String routeName;
    private final String displayClass;
    private int elapsedTime;
    private String serviceId;
    private String lastStation;

    public RawStage(String firstStation, String routeName, TransportMode mode, String displayClass, int elapsedTime) {
        this.firstStation = firstStation;
        this.routeName = routeName;
        this.mode = mode;
        this.displayClass = displayClass;
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

    public TransportMode getMode() {
        return mode;
    }

    public String getDisplayClass() {
        return displayClass;
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
                ", displayClass='" + displayClass + '\'' +
                ", serviceId='" + serviceId + '\'' +
                ", lastStation='" + lastStation + '\'' +
                '}';
    }

    public int getElapsedTime() {
        return elapsedTime;
    }
}
