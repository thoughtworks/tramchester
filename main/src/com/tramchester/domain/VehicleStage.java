package com.tramchester.domain;

import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;

import java.util.Objects;

public class VehicleStage implements TransportStage<Station, Station> {
    private final Station firstStation;
    private final Station lastStation;

    protected final TransportMode mode;
    private final boolean hasPlatforms;
    private final Trip trip;
    private final int passedStops;
    private final TramTime departFirstStationTime;
    private final Route route;

    protected int cost;
    private Platform platform;

    public VehicleStage(Station firstStation, Route route, TransportMode mode, Trip trip,
                        TramTime departFirstStationTime, Station lastStation, int passedStops,
                        boolean hasPlatforms) {
        this.firstStation = firstStation;
        this.route = route;
        this.mode = mode;
        this.hasPlatforms = hasPlatforms;
        this.platform = null;
        this.trip = trip;
        this.departFirstStationTime = departFirstStationTime;
        this.lastStation = lastStation;
        this.passedStops = passedStops;
    }

    public Station getFirstStation() {
        return firstStation;
    }

    @Override
    public int getDuration() {
        return cost;
    }

    public Station getLastStation() {
        return lastStation;
    }

    public Location<?> getActionStation() { return firstStation; }

    @Override
    public String getHeadSign() {
        return trip.getHeadsign();
    }

    public String getRouteName() {
        return route.getName();
    }

    @Override
    public String getRouteShortName() {
        return route.getShortName();
    }

    @Override
    public TransportMode getMode() {
        return mode;
    }

    public VehicleStage setCost(int cost) {
        this.cost = cost;
        return this;
    }

    public void setPlatform(Platform platform) {
        if (!hasPlatforms) {
            throw new RuntimeException("Adding platforms to a non-platform vehical stage " + toString());
        }
        this.platform = platform;
    }

    public Platform getBoardingPlatform() {
        if (platform==null) {
            throw new RuntimeException("No platform");
        }
        return platform;
    }

    @Override
    public boolean hasBoardingPlatform() {
        return hasPlatforms;
    }

    public int getCost() {
        return cost;
    }

    @Deprecated
    public TramTime getDepartTime() {
        return departFirstStationTime;
    }

    @Override
    public TramTime getFirstDepartureTime() {
        return departFirstStationTime;
    }

    @Override
    public TramTime getExpectedArrivalTime() {
        return departFirstStationTime.plusMinutes(cost);
    }

    public int getPassedStops() {
        return passedStops;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VehicleStage that = (VehicleStage) o;
        return passedStops == that.passedStops &&
                cost == that.cost &&
                firstStation.equals(that.firstStation) &&
                lastStation.equals(that.lastStation) &&
                mode == that.mode &&
                trip.equals(that.trip) &&
                departFirstStationTime.equals(that.departFirstStationTime) &&
                Objects.equals(platform, that.platform);
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstStation, lastStation, mode, trip, passedStops, departFirstStationTime, cost, platform);
    }

    @Override
    public String toString() {
        return "VehicleStage{" +
                "firstStation=" + firstStation.getName() +
                ", lastStation=" + lastStation.getName() +
                ", mode=" + mode +
                ", routeId='" + route.getId() + '\'' +
                ", tripId=" + trip.getId() +
                ", passedStops=" + passedStops +
                ", departTime=" + departFirstStationTime +
                ", cost=" + cost +
                ", platform=" + platform +
                '}';
    }
}
