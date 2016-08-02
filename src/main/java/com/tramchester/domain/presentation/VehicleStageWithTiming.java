package com.tramchester.domain.presentation;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.domain.Location;
import com.tramchester.domain.RawVehicleStage;
import com.tramchester.domain.TimeAsMinutes;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.mappers.TimeJsonSerializer;

import java.time.LocalTime;
import java.util.SortedSet;

import static java.lang.String.format;

public class VehicleStageWithTiming extends RawVehicleStage implements PresentableStage {
    private final TravelAction action;
    private SortedSet<ServiceTime> serviceTimes;

    public VehicleStageWithTiming(RawVehicleStage rawTravelStage, SortedSet<ServiceTime> serviceTimes, TravelAction action) {
        super(rawTravelStage);
        this.serviceTimes = serviceTimes;
        this.action = action;
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

    public int getDuration() {
//        // likely this only works for Tram when duration between stops does not vary by time of day
//        ServiceTime serviceTime = serviceTimes.first();
//        LocalTime arrivalTime = serviceTime.getArrivalTime();
//        LocalTime departureTime = serviceTime.getDepartureTime();
//
//        return TimeAsMinutes.timeDiffMinutes(arrivalTime, departureTime);
        return cost;
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
        return "VehicleStageWithTiming{" +
                "rawTravelStage=" + super.toString() +
                ", serviceTimes=" + serviceTimes +
                '}';
    }

    public String getSummary() throws TramchesterException {
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

    public String getPrompt() throws TramchesterException {
        String verb;
        switch (action) {
            case Board: verb = "Board";
                break;
            case Leave: verb = "Leave";
                break;
            case Change: verb = "Change";
                break;
            default:
                throw new TramchesterException("Unknown transport action " + action);
        }

        switch (mode) {
            case Bus : {
                return format("%s bus at", verb);
            }
            case Tram: {
                return format("%s tram at", verb);
            }
            default:
                throw new TramchesterException("Unknown transport mode " + mode);
        }
    }

    @Override
    public int getNumberOfServiceTimes() {
        return serviceTimes.size();
    }

}
