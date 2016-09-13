package com.tramchester.domain.presentation;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.domain.RawVehicleStage;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.mappers.TimeJsonSerializer;
import org.joda.time.LocalTime;

import static java.lang.String.format;

public class VehicleStageWithTiming extends RawVehicleStage implements PresentableStage {
    private TravelAction action;
    private ServiceTime serviceTime;

    public VehicleStageWithTiming(RawVehicleStage rawTravelStage, ServiceTime serviceTime, TravelAction action) {
        super(rawTravelStage);
        this.action = action;
        this.serviceTime = serviceTime;
    }

    public VehicleStageWithTiming() {
        // deserialisation
    }

    @JsonSerialize(using = TimeJsonSerializer.class)
    public LocalTime getExpectedArrivalTime() {
        return serviceTime.getArrivalTime();
    }

    @JsonSerialize(using = TimeJsonSerializer.class)
    public LocalTime getFirstDepartureTime() {
       return serviceTime.getDepartureTime();
    }

    public int getDuration() {
        return cost;
    }

    public int findEarliestDepartureTime() {
        return serviceTime.getFromMidnightLeaves();
    }

    @Override
    public String toString() {
        return "VehicleStageWithTiming{" +
                "rawTravelStage=" + super.toString() +
                ", serviceTime=" + serviceTime +
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
    public String getHeadSign() {
        return serviceTime.getHeadSign();
    }


}
