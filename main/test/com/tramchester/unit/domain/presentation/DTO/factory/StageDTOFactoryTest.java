package com.tramchester.unit.domain.presentation.DTO.factory;

import com.tramchester.domain.*;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.presentation.DTO.PlatformDTO;
import com.tramchester.domain.presentation.DTO.StageDTO;
import com.tramchester.domain.presentation.DTO.factory.StageDTOFactory;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.presentation.TravelAction;
import com.tramchester.integration.Stations;
import com.tramchester.livedata.EnrichPlatform;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;

import static org.junit.Assert.assertEquals;

public class StageDTOFactoryTest extends EasyMockSupport {

    private StageDTOFactory factory;
    private EnrichPlatform enrichPlatform;

    @Before
    public void beforeEachTestRun() {
        enrichPlatform = createMock(EnrichPlatform.class);

        factory = new StageDTOFactory(enrichPlatform);
    }

    @Test
    public void shouldCreateStageDTOCorrectlyForWalking() {
        WalkingStage stage = new WalkingStage(Stations.Altrincham, Stations.NavigationRoad, 15, TramTime.of(8,11));

        TramServiceDate tramServiceDate = new TramServiceDate(LocalDate.now());
        StageDTO build = factory.build(stage, TravelAction.Walk, TramTime.of(8,0), tramServiceDate);
        replayAll();
        checkValues(stage, build, false, TravelAction.Walk);
        verifyAll();
    }

    @Test
    public void shouldCreateStageDTOCorrectlyForTransportStage() {
        Trip trip = new Trip("tripId", "headSign", "svcId", "routeName");
        VehicleStage vehicleStage = new VehicleStage(Stations.MarketStreet, "routeName",
                TransportMode.Tram, "Displayclass", trip, TramTime.of(0, 0), Stations.Bury, 23);
        vehicleStage.setCost(5);

        Platform platform = new Platform("platFormId", "platformName");
        vehicleStage.setPlatform(platform);

        TramServiceDate tramServiceDate = new TramServiceDate(LocalDate.now());
        enrichPlatform.enrich(new PlatformDTO(platform), tramServiceDate, TramTime.of(8, 23));
        EasyMock.expectLastCall();

        replayAll();
        StageDTO stageDTO = factory.build(vehicleStage, TravelAction.Board, TramTime.of(8, 23), tramServiceDate);
        verifyAll();

        checkValues(vehicleStage, stageDTO, true, TravelAction.Board);
    }

    private void checkValues(TransportStage stage, StageDTO dto, boolean hasPlatform, TravelAction action) {
        assertEquals(stage.getActionStation().getId(), dto.getActionStation().getId());
        assertEquals(stage.getMode(), dto.getMode());
        assertEquals(stage.getFirstDepartureTime(), dto.getFirstDepartureTime());
        assertEquals(stage.getLastStation().getId(), dto.getLastStation().getId());
        assertEquals(stage.getExpectedArrivalTime(), dto.getExpectedArrivalTime());
        assertEquals(stage.getDuration(), dto.getDuration());
        assertEquals(stage.getFirstStation().getId(), dto.getFirstStation().getId());
        assertEquals(stage.getHeadSign(), dto.getHeadSign());
        assertEquals(stage.getRouteName(), dto.getRouteName());
        assertEquals(stage.getDisplayClass(), dto.getDisplayClass());
        assertEquals(stage.getPassedStops(), dto.getPassedStops());
        assertEquals(action.toString(), dto.getAction());
        assertEquals(hasPlatform, dto.getHasPlatform());
    }
}
