package com.tramchester.domain.presentation;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.domain.RawStage;
import com.tramchester.mappers.TimeJsonSerializer;

import java.time.LocalTime;
import java.util.SortedSet;

public class Stage {
    public static final int SECONDS_IN_DAY = 24*60*60;
    private final RawStage rawStage;
    private SortedSet<ServiceTime> serviceTimes;

    public Stage(RawStage rawStage, SortedSet<ServiceTime> serviceTimes) {
        this.rawStage = rawStage;
        this.serviceTimes = serviceTimes;
    }

    public String getFirstStation() {
        return rawStage.getFirstStation();
    }

    public String getRoute() {
        return rawStage.getRouteName();
    }

    public String getLastStation() {
        return rawStage.getLastStation();
    }

    // used from javascript on front-end
    public String getDisplayClass() {
        return rawStage.getDisplayClass();
    }

    public String getMode() {
        return rawStage.getMode();
    }

    public String getServiceId() {
        return rawStage.getServiceId();
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
        return "Stage{" +
                "rawStage=" + rawStage +
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
}
