package com.tramchester.dataimport.rail;

import com.tramchester.dataimport.rail.records.BasicSchedule;
import com.tramchester.dataimport.rail.records.reference.TrainCategory;
import com.tramchester.dataimport.rail.records.reference.TrainStatus;
import com.tramchester.domain.reference.TransportMode;

import static com.tramchester.domain.reference.TransportMode.*;

public class RailTransportModeMapper {

    public static TransportMode getModeFor(TrainStatus status, TrainCategory category) {
        return switch (status) {
            case Bus, STPBus -> getBusModeFor(category);
            case STPPassengerParcels, PassengerAndParcels -> getRailOrSubwayMode(category);
            case Freight, STPFreight -> Train;
            case Ship, STPShip -> TransportMode.Ship;
            case Trip, STPTrip, Unknown -> TransportMode.Unknown; // Trip seems unused?
        };
    }

    public static TransportMode getModeFor(BasicSchedule basicScheduleRecord) {
        return getModeFor(basicScheduleRecord.getTrainStatus(), basicScheduleRecord.getTrainCategory());
    }

    private static TransportMode getRailOrSubwayMode(TrainCategory category) {
        if (category==TrainCategory.LondonUndergroundOrMetroService) {
            return Subway;
        }
        return Train;
    }

    private static TransportMode getBusModeFor(TrainCategory category) {
        if (category==TrainCategory.BusReplacement) {
            return RailReplacementBus;
        }
        return Bus;
    }

}
