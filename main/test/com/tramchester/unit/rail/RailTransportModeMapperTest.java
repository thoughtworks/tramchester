package com.tramchester.unit.rail;

import com.tramchester.dataimport.rail.RailTransportModeMapper;
import com.tramchester.dataimport.rail.records.reference.TrainCategory;
import com.tramchester.dataimport.rail.records.reference.TrainStatus;
import com.tramchester.domain.reference.TransportMode;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.Test;

import static com.tramchester.domain.reference.TransportMode.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class RailTransportModeMapperTest extends EasyMockSupport {

    @Test
    void shouldHaveCorrectModeForTrain() {
        TransportMode result = RailTransportModeMapper.getModeFor(TrainStatus.PassengerAndParcels, TrainCategory.OrdinaryPassenger);
        assertEquals(Train, result);
    }

    @Test
    void shouldHaveCorrectModeForRailReplacement() {
        TransportMode resultA = RailTransportModeMapper.getModeFor(TrainStatus.Bus, TrainCategory.BusReplacement);
        assertEquals(RailReplacementBus, resultA);

        TransportMode resultB = RailTransportModeMapper.getModeFor(TrainStatus.STPBus, TrainCategory.BusReplacement);
        assertEquals(RailReplacementBus, resultB);
    }

    @Test
    void shouldHaveCorrectModeForBus() {
        TransportMode result = RailTransportModeMapper.getModeFor(TrainStatus.Bus, TrainCategory.BusService);
        assertEquals(Bus, result);
    }

    @Test
    void shouldHaveCorrectModeForLondonUnderground() {
        TransportMode resultA = RailTransportModeMapper.getModeFor(TrainStatus.PassengerAndParcels, TrainCategory.LondonUndergroundOrMetroService);
        assertEquals(TransportMode.Subway, resultA);

        TransportMode resultB = RailTransportModeMapper.getModeFor(TrainStatus.STPPassengerParcels, TrainCategory.LondonUndergroundOrMetroService);
        assertEquals(TransportMode.Subway, resultB);
    }

    @Test
    void shouldHaveCorrectModeForShip() {
        TransportMode result = RailTransportModeMapper.getModeFor(TrainStatus.Ship, TrainCategory.Unknown);
        assertEquals(Ship, result);
    }

}
