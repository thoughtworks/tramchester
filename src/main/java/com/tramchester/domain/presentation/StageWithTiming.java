package com.tramchester.domain.presentation;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.domain.*;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.mappers.StationSerializer;
import com.tramchester.mappers.TimeJsonSerializer;
import com.tramchester.mappers.TransportModeSerializer;

import java.time.LocalTime;
import java.util.SortedSet;

import static java.lang.String.format;

public class StageWithTiming implements VehicleStage {
    public static final int SECONDS_IN_DAY = 24*60*60;
    private final RawVehicleStage rawTravelStage;
    private SortedSet<ServiceTime> serviceTimes;

    public StageWithTiming(RawVehicleStage rawTravelStage, SortedSet<ServiceTime> serviceTimes) {
        this.rawTravelStage = rawTravelStage;
        this.serviceTimes = serviceTimes;
    }

    @Override
    @JsonSerialize(using = StationSerializer.class)
    public Station getFirstStation() {
        return rawTravelStage.getFirstStation();
    }

    @Override
    public String getRouteName() {
        return rawTravelStage.getRouteName();
    }

    @Override
    @JsonSerialize(using = StationSerializer.class)
    public Station getLastStation() {
        return rawTravelStage.getLastStation();
    }

    // used from javascript on front-end
    public String getDisplayClass() {
        return rawTravelStage.getDisplayClass();
    }

    @JsonSerialize(using = TransportModeSerializer.class)
    public TransportMode getMode() {
        return rawTravelStage.getMode();
    }

    @Override
    public boolean isVehicle() {
        return rawTravelStage.isVehicle();
    }

    public String getServiceId() {
        return rawTravelStage.getServiceId();
    }

    public SortedSet<ServiceTime> getServiceTimes() {
        return serviceTimes;
    }

    @JsonSerialize(using = TimeJsonSerializer.class)
    public LocalTime getExpectedArrivalTime() {
        return serviceTimes.first().getArrivalTime();
    }

    @JsonSerialize(using = TimeJsonSerializer.class)
    public LocalTime getFirstDepartureTime() {
       return serviceTimes.first().getDepartureTime();
    }

    // this is wrong, duration varies, need to extract from servicetime instead
    @Deprecated
    public int getDuration() {
        // likely this only works for Tram when duration between stops does not vary by time of day
        ServiceTime serviceTime = serviceTimes.first();
        LocalTime arrivalTime = serviceTime.getArrivalTime();
        LocalTime departureTime = serviceTime.getDepartureTime();
        int depSecs = departureTime.toSecondOfDay();

        int seconds;
        if (arrivalTime.isBefore(departureTime)) { // crosses midnight
            int secsBeforeMid = SECONDS_IN_DAY - depSecs;
            int secsAfterMid = arrivalTime.toSecondOfDay();
            seconds = secsBeforeMid + secsAfterMid;
        } else {
            seconds = arrivalTime.toSecondOfDay() - depSecs;
        }
        return seconds / 60;
    }

    public int findEarliestDepartureTime() {
        int earliest = Integer.MAX_VALUE;
        for (ServiceTime time : serviceTimes) {
            if (time.getFromMidnightLeaves() < earliest) {
                earliest = time.getFromMidnightLeaves();
            }
        }
        return earliest;
    }

    @Override
    public String toString() {
        return "StageWithTiming{" +
                "rawTravelStage=" + rawTravelStage +
                ", serviceTimes=" + serviceTimes +
                '}';
    }

    @Deprecated
    public int findDepartureTimeForEarliestArrival() {
        int depart = Integer.MAX_VALUE;
        int earlierArrive = Integer.MAX_VALUE;
        for (ServiceTime time : serviceTimes) {
            if (time.getFromMidnightArrives() < earlierArrive) {
                depart = time.getFromMidnightLeaves();
            }
        }
        return depart;
    }

    public String getSummary() throws TramchesterException {
        TransportMode mode = rawTravelStage.getMode();
        String routeName = rawTravelStage.getRouteName();
        switch (mode) {
            case Bus : {
                return format("%s Bus route", routeName);
            }
            case Tram: {
                return format("%s Tram line", routeName);
            }
            default:
                throw new TramchesterException("Unknown transport mode " + mode);
        }
    }

    public String getPrompt() {
        return "Walk to";
    }
}
