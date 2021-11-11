package com.tramchester.domain.transportStages;

import com.tramchester.domain.Platform;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.MutableTrip;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;

import java.util.List;
import java.util.stream.Collectors;

import static com.tramchester.domain.id.HasId.asId;

public class VehicleStage implements TransportStage<Station, Station> {
    private final Station firstStation;
    private final Station lastStation;

    protected final TransportMode mode;
    private final List<Integer> stopSequenceNumbers;
    private final Trip trip;
    private final TramTime departFirstStationTime;
    private final Route route;

    protected int cost;
    private Platform platform;

    public VehicleStage(Station firstStation, Route route, TransportMode mode, Trip trip,
                        TramTime departFirstStationTime, Station lastStation,
                        List<Integer> stopSequenceNumbers) {
        this.firstStation = firstStation;
        this.route = route;
        this.mode = mode;
        this.stopSequenceNumbers = stopSequenceNumbers;
        this.trip = trip;
        this.departFirstStationTime = departFirstStationTime;
        this.lastStation = lastStation;

        this.platform = null;
        this.cost = Integer.MAX_VALUE;
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
        if (!firstStation.hasPlatforms()) {
            throw new RuntimeException("Adding platforms to a zero-platform vehicle stage " + this);
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
        return platform != null;
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
                map(seqNum -> trip.getStopCalls().getStopBySequenceNumber(seqNum)).
                filter(StopCall::callsAtStation).
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

        VehicleStage stage = (VehicleStage) o;

        if (!firstStation.equals(stage.firstStation)) return false;
        if (!lastStation.equals(stage.lastStation)) return false;
        return trip.equals(stage.trip);
    }

    @Override
    public int hashCode() {
        int result = firstStation.hashCode();
        result = 31 * result + lastStation.hashCode();
        result = 31 * result + trip.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "VehicleStage{" +
                "firstStation=" + asId(firstStation) +
                ", lastStation=" + asId(lastStation) +
                ", mode=" + mode +
                ", passedStations=" + stopSequenceNumbers +
                ", trip=" + asId(trip) +
                ", departFirstStationTime=" + departFirstStationTime +
                ", route=" + asId(route) +
                ", cost=" + cost +
                '}';
    }
}
