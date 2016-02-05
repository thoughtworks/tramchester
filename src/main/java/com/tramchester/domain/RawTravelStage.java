package com.tramchester.domain;

public class RawTravelStage implements RawStage {
    private final Station firstStation;
    private final TransportMode mode;
    private final String routeName;
    private final String displayClass;
    private int elapsedTime;
    private String serviceId;
    private Station lastStation;

    public RawTravelStage(Station firstStation, String routeName, TransportMode mode, String displayClass, int elapsedTime) {
        this.firstStation = firstStation;
        this.routeName = routeName;
        this.mode = mode;
        this.displayClass = displayClass;
        this.elapsedTime = elapsedTime;
    }

    public String getServiceId() {
        return serviceId;
    }

    public RawTravelStage setServiceId(String serviceId) {
        this.serviceId = serviceId;
        return this;
    }

    public RawTravelStage setLastStation(Station lastStation) {
        this.lastStation = lastStation;
        return this;
    }

    public Station getFirstStation() {
        return firstStation;
    }

    public Station getLastStation() {
        return lastStation;
    }

    public String getRouteName() {
        return routeName;
    }

    @Override
    public TransportMode getMode() {
        return mode;
    }

    public String getDisplayClass() {
        return displayClass;
    }


    @Override
    public String toString() {
        return "RawTravelStage{" +
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
