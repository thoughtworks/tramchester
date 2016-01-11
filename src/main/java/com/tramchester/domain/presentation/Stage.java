package com.tramchester.domain.presentation;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.domain.RawStage;
import com.tramchester.mappers.TimeJsonSerializer;

import java.time.LocalTime;
import java.util.List;

public class Stage {
    public static final int SECONDS_IN_DAY = 24*60*60;
    private final String mode;
    private String firstStation;
    private String route;
    private String lastStation;
    private String serviceId;
    private List<ServiceTime> serviceTimes;
    private String tramRouteId;

    // use RawStage
    @Deprecated
    public Stage(String firstStation, String routeName, String mode, String tramRouteId) {
        this.firstStation = firstStation;
        this.route = routeName;
        this.mode = mode;
        this.tramRouteId = tramRouteId;
    }

    public Stage(RawStage rawStage) {
        this.firstStation = rawStage.getFirstStation();
        this.route = rawStage.getRouteName();
        this.mode = rawStage.getMode();
        this.tramRouteId = rawStage.getRouteId();
        this.lastStation = rawStage.getLastStation();
        this.firstStation = rawStage.getFirstStation();
        this.serviceId = rawStage.getServiceId();
    }

    public String getFirstStation() {
        return firstStation;
    }

    public String getRoute() {
        return route;
    }

    public String getLastStation() {
        return lastStation;
    }

    // used from javascript on front-end
    // TODO use the full route ID on front end
    public String getTramRouteId() {
        return tramRouteId;
    }

//    public void setServiceId(String serviceId) {
//        this.serviceId = serviceId;
//    }

    public String getServiceId() {
        return serviceId;
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
                "firstStation='" + firstStation + '\'' +
                ", route='" + route + '\'' +
                ", tramRouteId='" + tramRouteId + '\'' +
                ", lastStation='" + lastStation + '\'' +
                ", serviceId='" + serviceId + '\'' +
                ", serviceTimes=" + serviceTimes +
                '}';
    }

    public String getMode() {
        return mode;
    }
}
