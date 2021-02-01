package com.tramchester.domain;

import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;

import java.util.List;
import java.util.stream.Collectors;

import static com.tramchester.domain.HasId.asId;
import static com.tramchester.domain.reference.GTFSPickupDropoffType.None;

public class VehicleStage implements TransportStage<Station, Station> {
    private final Station firstStation;
    private final Station lastStation;

    protected final TransportMode mode;
    private final List<Integer> stopSequenceNumbers;
    private final boolean hasPlatforms;
    private final Trip trip;
    private final TramTime departFirstStationTime;
    private final Route route;

    protected int cost;
    private Platform platform;

    public VehicleStage(Station firstStation, Route route, TransportMode mode, Trip trip,
                        TramTime departFirstStationTime, Station lastStation,
                        List<Integer> stopSequenceNumbers,
                        boolean hasPlatforms) {
        this.firstStation = firstStation;
        this.route = route;
        this.mode = mode;
        this.stopSequenceNumbers = stopSequenceNumbers;
        this.hasPlatforms = hasPlatforms;
        this.platform = null;
        this.trip = trip;
        this.departFirstStationTime = departFirstStationTime;
        this.lastStation = lastStation;
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

    public Route getRoute() {
        return route;
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
            throw new RuntimeException("Adding platforms to a zero-platform vehicle stage " + toString());
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

    public int getPassedStopsCount() {
        return stopSequenceNumbers.size();
    }

    @Override
    public List<StopCall> getCallingPoints() {
        return stopSequenceNumbers.stream().
                map(seqNum -> trip.getStops().getStopBySequenceNumber(seqNum)).
                filter(stopCall -> stopCall.callsAtStation()).
                collect(Collectors.toList());
    }

    @Override
    public IdFor<Trip> getTripId() {
        return trip.getId();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VehicleStage that = (VehicleStage) o;

        if (hasPlatforms != that.hasPlatforms) return false;
        if (cost != that.cost) return false;
        if (!firstStation.equals(that.firstStation)) return false;
        if (!lastStation.equals(that.lastStation)) return false;
        if (mode != that.mode) return false;
        if (!stopSequenceNumbers.equals(that.stopSequenceNumbers)) return false;
        if (!trip.equals(that.trip)) return false;
        if (!departFirstStationTime.equals(that.departFirstStationTime)) return false;
        if (!route.equals(that.route)) return false;
        return platform.equals(that.platform);
    }

    @Override
    public int hashCode() {
        int result = firstStation.hashCode();
        result = 31 * result + lastStation.hashCode();
        result = 31 * result + mode.hashCode();
        result = 31 * result + stopSequenceNumbers.hashCode();
        result = 31 * result + (hasPlatforms ? 1 : 0);
        result = 31 * result + trip.hashCode();
        result = 31 * result + departFirstStationTime.hashCode();
        result = 31 * result + route.hashCode();
        result = 31 * result + cost;
        result = 31 * result + platform.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "VehicleStage{" +
                "firstStation=" + asId(firstStation) +
                ", lastStation=" + asId(lastStation) +
                ", mode=" + mode +
                ", passedStations=" + stopSequenceNumbers +
                ", hasPlatforms=" + hasPlatforms +
                ", trip=" + asId(trip) +
                ", departFirstStationTime=" + departFirstStationTime +
                ", route=" + asId(route) +
                ", cost=" + cost +
                '}';
    }
}
