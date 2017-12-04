package com.tramchester.domain;

import java.util.Optional;

public class RawVehicleStage implements RawStage {
    protected Location firstStation;
    protected TransportMode mode;
    protected String routeName;
    protected String displayClass;
    protected int cost;

    protected String serviceId;
    protected Location lastStation;
    private Optional<Platform> platform;

    public RawVehicleStage(Location firstStation, String routeName, TransportMode mode, String displayClass) {
        this.firstStation = firstStation;
        this.routeName = routeName;
        this.mode = mode;
        this.displayClass = displayClass;
        platform = Optional.empty();
    }

    public RawVehicleStage() {
        // for deserialisation
    }

    public RawVehicleStage(RawVehicleStage other) {
        this.firstStation = other.firstStation;
        this.routeName = other.routeName;
        this.mode = other.mode;
        this.displayClass = other.displayClass;
        this.serviceId = other.serviceId;
        this.lastStation = other.lastStation;
        this.cost = other.cost;
        this.platform = other.platform;
    }

    public String getServiceId() {
        return serviceId;
    }

    public RawVehicleStage setServiceId(String serviceId) {
        this.serviceId = serviceId;
        return this;
    }

    public RawVehicleStage setLastStation(Location lastStation) {
        this.lastStation = lastStation;
        return this;
    }

    public Location getFirstStation() {
        return firstStation;
    }

    public Location getLastStation() {
        return lastStation;
    }

    public Location getActionStation() { return firstStation; }

    public String getRouteName() {
        return routeName;
    }

    @Override
    public TransportMode getMode() {
        return mode;
    }

    @Override
    public boolean getIsAVehicle() {
        return (mode.equals(TransportMode.Bus)) || (mode.equals(TransportMode.Tram));
    }

    @Override
    public boolean isWalk() {
        return mode.equals(TransportMode.Walk);
    }

    public String getDisplayClass() {
        return displayClass;
    }

    @Override
    public String toString() {
        return "RawVehicleStage{" +
                "firstStation=" + firstStation +
                ", mode=" + mode +
                ", routeName='" + routeName + '\'' +
                ", displayClass='" + displayClass + '\'' +
                ", cost=" + cost +
                ", serviceId='" + serviceId + '\'' +
                ", lastStation=" + lastStation +
                ", platform=" + platform +
                '}';
    }

    public RawVehicleStage setCost(int cost) {
        this.cost = cost;
        return this;
    }

    public void setPlatform(Platform platform) {
        this.platform = Optional.of(platform);
    }

    public Optional<Platform> getPlatform() {
        return platform;
    }
}
