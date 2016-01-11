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

    public Stage(RawStage rawStage) {
        this.rawStage = rawStage;
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

    public void setServiceTimes(List<ServiceTime> times) {
        this.serviceTimes = times;
    }

    public List<ServiceTime> getServiceTimes() {
        return serviceTimes;
    }

    @JsonSerialize(using = TimeJsonSerializer.class)
    public LocalTime getExpectedArrivalTime() {
        LocalTime firstArrivalTime = LocalTime.now();
        if (serviceTimes.size() > 0) {
            firstArrivalTime = serviceTimes.get(0).getArrivalTime();
        }
        return firstArrivalTime;
    }

    @JsonSerialize(using = TimeJsonSerializer.class)
    public LocalTime getFirstDepartureTime() {
        LocalTime firstDepartureTime = LocalTime.now();
        if (serviceTimes.size() > 0) {
            firstDepartureTime = serviceTimes.get(0).getDepartureTime();
        }
        // TODO only ever reach here in error?
        return firstDepartureTime;
    }

    public int getDuration() {
        if (!serviceTimes.isEmpty()) {
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
        return 0;
    }

    @Override
    public String toString() {
        return "Stage{" +
                "rawStage=" + rawStage +
                ", serviceTimes=" + serviceTimes +
                '}';
    }
}
