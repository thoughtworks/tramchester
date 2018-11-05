package com.tramchester.domain.presentation;

import com.tramchester.domain.RawVehicleStage;
import com.tramchester.domain.TramTime;

import java.time.LocalTime;

import static java.lang.String.format;

public class VehicleStageWithTiming extends RawVehicleStage implements TransportStage {
    private TravelAction action;
    private ServiceTime serviceTime;

    public VehicleStageWithTiming(RawVehicleStage rawTravelStage, ServiceTime serviceTime, TravelAction action) {
        super(rawTravelStage);
        this.action = action;
        this.serviceTime = serviceTime;
    }

    public TramTime getExpectedArrivalTime() {
        return serviceTime.getArrivalTime();
    }

    public TramTime getFirstDepartureTime() {
       return serviceTime.getDepartureTime();
    }

    public int getDuration() {
        return cost;
    }

    public LocalTime findEarliestDepartureTime() {
        return serviceTime.getLeaves();
    }

    @Override
    public String toString() {
        return "VehicleStageWithTiming{" +
                "rawTravelStage=" + super.toString() +
                ", serviceTime=" + serviceTime +
                '}';
    }

    public String getSummary() {
        switch (mode) {
            case Bus : {
                return format("%s Bus route", routeName);
            }
            case Tram: {
                return format("%s Tram line", routeName);
            }
            default:
                return "Unknown transport mode " + mode;
        }
    }

    public String getPrompt() {
        String verb;
        switch (action) {
            case Board: verb = "Board";
                break;
            case Leave: verb = "Leave";
                break;
            case Change: verb = "Change";
                break;
            default:
                verb = "Unknown transport action " + action;
        }

        switch (mode) {
            case Bus : {
                return format("%s bus at", verb);
            }
            case Tram: {
                return format("%s tram at", verb);
            }
            default:
                return "Unknown transport mode " + mode;
        }
    }

    @Override
    public String getHeadSign() {
        return serviceTime.getHeadSign();
    }


}
