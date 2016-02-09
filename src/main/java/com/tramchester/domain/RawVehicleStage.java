package com.tramchester.domain;

public class RawVehicleStage implements VehicleStage {
    private final Station firstStation;
    private final TransportMode mode;
    private final String routeName;
    private final String displayClass;
    private int elapsedTime;
    private String serviceId;
    private Station lastStation;

    public RawVehicleStage(Station firstStation, String routeName, TransportMode mode, String displayClass, int elapsedTime) {
        this.firstStation = firstStation;
        this.routeName = routeName;
        this.mode = mode;
        this.displayClass = displayClass;
        this.elapsedTime = elapsedTime;
    }

    public String getServiceId() {
        return serviceId;
    }

    public RawVehicleStage setServiceId(String serviceId) {
        this.serviceId = serviceId;
        return this;
    }

    public RawVehicleStage setLastStation(Station lastStation) {
        this.lastStation = lastStation;
        return this;
    }

    @Override
    public Station getFirstStation() {
        return firstStation;
    }

    @Override
    public Station getLastStation() {
        return lastStation;
    }

    @Override
    public String getRouteName() {
        return routeName;
    }

    @Override
    public TransportMode getMode() {
        return mode;
    }

    @Override
    public boolean isVehicle() {
        return (mode.equals(TransportMode.Bus)) || (mode.equals(TransportMode.Tram));
    }

    public String getDisplayClass() {
        return displayClass;
    }

    @Override
    public String toString() {
        return "RawVehicleStage{" +
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
