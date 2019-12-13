package com.tramchester.domain;

import java.time.LocalTime;
import java.util.Objects;
import java.util.Optional;

public class RawVehicleStage implements RawStage {
    private Location firstStation;
    protected TransportMode mode;
    protected String routeName;
    private String displayClass;

    protected int cost;

    protected String serviceId;
    private Location lastStation;
    private int passedStops;
    private Optional<Platform> platform;

    private String tripId;
    private TramTime departTime;

    public RawVehicleStage(Location firstStation, String routeName, TransportMode mode, String displayClass) {
        this.firstStation = firstStation;
        this.routeName = routeName;
        this.mode = mode;
        this.displayClass = displayClass;
        platform = Optional.empty();
        passedStops = -1;
    }

    public RawVehicleStage(RawVehicleStage other) {
        this(other.firstStation, other.routeName, other.mode, other.displayClass);

        this.serviceId = other.serviceId;
        this.lastStation = other.lastStation;
        this.cost = other.cost;
        this.platform = other.platform;
        this.tripId = other.tripId;
        this.departTime = other.departTime;
        this.passedStops = other.passedStops;
    }

    public String getServiceId() {
        return serviceId;
    }

    public RawVehicleStage setServiceId(String serviceId) {
        this.serviceId = serviceId;
        return this;
    }

    public RawVehicleStage setLastStation(Location lastStation, int passedStops) {
        this.lastStation = lastStation;
        this.passedStops = passedStops;
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
                ", tripId='" + tripId + '\'' +
                ", departTime=" + departTime +
                '}';
    }

    public RawVehicleStage setCost(int cost) {
        this.cost = cost;
        return this;
    }

    public void setPlatform(Platform platform) {
        this.platform = Optional.of(platform);
    }

    public Optional<Platform> getBoardingPlatform() {
        return platform;
    }

    @Deprecated
    public int getCost() {
        return cost;
    }

    public void setTripId(String id) {
        tripId = id;
    }

    public void setDepartTime(TramTime time) {
        departTime = time;
    }

    public TramTime getDepartTime() {
        return departTime;
    }

    public String getTripId() {
        return tripId;
    }

    public int getPassedStops() {
        return passedStops;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RawVehicleStage that = (RawVehicleStage) o;
        return firstStation.equals(that.firstStation) &&
                Objects.equals(serviceId, that.serviceId) &&
                Objects.equals(lastStation, that.lastStation) &&
                Objects.equals(tripId, that.tripId) &&
                Objects.equals(departTime, that.departTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstStation, serviceId, lastStation, tripId, departTime);
    }

}
