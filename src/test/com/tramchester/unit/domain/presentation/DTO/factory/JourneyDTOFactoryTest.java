package com.tramchester.unit.domain.presentation.DTO.factory;

import com.tramchester.domain.*;
import com.tramchester.domain.presentation.*;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.StageDTO;
import com.tramchester.domain.presentation.DTO.factory.JourneyDTOFactory;
import com.tramchester.domain.presentation.DTO.factory.StageDTOFactory;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.joda.time.LocalTime;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class JourneyDTOFactoryTest extends EasyMockSupport {
    private Location stationA = new Station("stationA", "area", "nameA", new LatLong(-2, -1), false);
    private Location stationB = new Station("stationB", "area", "nameB", new LatLong(-3, 1), false);
    private StageDTOFactory stageFactory;

    @Before
    public void beforeEachTestRuns() {
        stageFactory = createMock(StageDTOFactory.class);
    }

    @Test
    public void shouldCreateJourneyDTO() {
        JourneyDTOFactory factory = new JourneyDTOFactory(stageFactory);
        TransportStage transportStage = createStage(new LocalTime(10, 20));
        Journey journey = new Journey(Arrays.asList(transportStage));

        StageDTO stageDTO = new StageDTO();
        EasyMock.expect(stageFactory.build(transportStage)).andReturn(stageDTO);

        replayAll();
        JourneyDTO journeyDTO = factory.build(journey);
        verifyAll();

        assertEquals(journey.getSummary(), journeyDTO.getSummary());
        assertEquals(journey.getHeading(), journeyDTO.getHeading());
        assertEquals(journey.getExpectedArrivalTime(), journeyDTO.getExpectedArrivalTime());
        assertEquals(journey.getFirstDepartureTime(), journeyDTO.getFirstDepartureTime());
        assertEquals(journey.getBegin().getId(), journeyDTO.getBegin().getId());
        assertEquals(journey.getEnd().getId(), journeyDTO.getEnd().getId());

        assertEquals(journey.getStages().size(),journeyDTO.getStages().size());
        assertEquals(stageDTO, journeyDTO.getStages().get(0));
    }


    private TransportStage createStage(LocalTime arrivesEnd) {
        RawVehicleStage rawTravelStage = new RawVehicleStage(stationA, "routeName", TransportMode.Bus, "cssClass").
                setLastStation(stationB).setCost(42);
        ServiceTime serviceTime = new ServiceTime(new LocalTime(10, 8), arrivesEnd, "svcId", "headSign", "tripId");
        VehicleStageWithTiming vehicleStageWithTiming = new VehicleStageWithTiming(rawTravelStage, serviceTime, TravelAction.Board);
        return vehicleStageWithTiming;
    }
}
