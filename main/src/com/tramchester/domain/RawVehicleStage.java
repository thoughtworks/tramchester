package com.tramchester.domain;

import com.tramchester.domain.input.Trip;
import com.tramchester.domain.presentation.TransportStage;

import java.util.Objects;
import java.util.Optional;

public class RawVehicleStage implements RawStage, TransportStage {
    private final Location firstStation;
    protected final TransportMode mode;
    protected final String routeName;
    private final String displayClass;
    private final String headsign;
    protected final String serviceId;
    private final String tripId;

    protected int cost;

    private Location lastStation;
    private int passedStops;

    private Optional<Platform> platform;

    private TramTime departTime;

    public RawVehicleStage(Location firstStation, String routeName, TransportMode mode, String displayClass, Trip trip) {
        this.firstStation = firstStation;
        this.routeName = routeName;
        this.mode = mode;
        this.displayClass = displayClass;
        this.platform = Optional.empty();
        this.passedStops = -1;

        // TODO Refactor to store trip and expose that instead
        this.headsign = trip.getHeadsign();
        this.serviceId = trip.getServiceId();
        this.tripId = trip.getTripId();
    }

//    @Deprecated
//    public RawVehicleStage(RawVehicleStage other) {
//        this(other.firstStation, other.routeName, other.mode, other.displayClass, other.headsign);
//
//        this.serviceId = other.serviceId;
//        this.lastStation = other.lastStation;
//        this.cost = other.cost;
//        this.platform = other.platform;
//        this.tripId = other.tripId;
//        this.departTime = other.departTime;
//        this.passedStops = other.passedStops;
//    }

    public String getServiceId() {
        return serviceId;
    }

//    public RawVehicleStage setServiceId(String serviceId) {
//        this.serviceId = serviceId;
//        return this;
//    }

    // into cons
    @Deprecated
    public RawVehicleStage setLastStation(Location lastStation, int passedStops) {
        this.lastStation = lastStation;
        this.passedStops = passedStops;
        return this;
    }

    public Location getFirstStation() {
        return firstStation;
    }

    @Override
    public int getDuration() {
        return cost;
    }

    public Location getLastStation() {
        return lastStation;
    }

    public Location getActionStation() { return firstStation; }

    @Override
    public String getHeadSign() {
        return headsign;
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

    public int getCost() {
        return cost;
    }

//    public void setTripId(String id) {
//        tripId = id;
//    }

    // into cons
    @Deprecated
    public void setDepartTime(TramTime time) {
        departTime = time;
    }

    public TramTime getDepartTime() {
        return departTime;
    }

    @Override
    public TramTime getFirstDepartureTime() {
        return departTime;
    }

    @Override
    public TramTime getExpectedArrivalTime() {
        return departTime.plusMinutes(cost);
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
