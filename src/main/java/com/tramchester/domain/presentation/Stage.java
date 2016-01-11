package com.tramchester.domain.presentation;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.domain.RawStage;
import com.tramchester.mappers.TimeJsonSerializer;

import java.time.LocalTime;
import java.util.List;

public class Stage {
    public static final int SECONDS_IN_DAY = 24*60*60;
    private final RawStage rawStage;
    private List<ServiceTime> serviceTimes;

    public Stage(RawStage rawStage, List<ServiceTime> serviceTimes) {
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
    public String getTramRouteId() {
        return rawStage.getRouteId();
    }

    public String getMode() {
        return rawStage.getMode();
    }

    public String getServiceId() {
        return rawStage.getServiceId();
    }

    public List<ServiceTime> getServiceTimes() {
        return serviceTimes;
    }

    @JsonSerialize(using = TimeJsonSerializer.class)
    public LocalTime getExpectedArrivalTime() {
        return serviceTimes.get(0).getArrivalTime();
    }

    @JsonSerialize(using = TimeJsonSerializer.class)
    public LocalTime getFirstDepartureTime() {
       return serviceTimes.get(0).getDepartureTime();
    }

    public int getDuration() {
        // likely this only works for Tram when duration between stops does not vary by time of day
        ServiceTime serviceTime = serviceTimes.get(0);
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
            if (time.getFromMidnight() < earliest) {
                earliest = time.getFromMidnight();
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

}
