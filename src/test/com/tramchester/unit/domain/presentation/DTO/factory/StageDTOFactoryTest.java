package com.tramchester.unit.domain.presentation.DTO.factory;

import com.tramchester.domain.*;
import com.tramchester.domain.presentation.DTO.PlatformDTO;
import com.tramchester.domain.presentation.DTO.StageDTO;
import com.tramchester.domain.presentation.DTO.factory.StageDTOFactory;
import com.tramchester.domain.presentation.ServiceTime;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.presentation.TravelAction;
import com.tramchester.domain.presentation.VehicleStageWithTiming;
import com.tramchester.integration.Stations;
import com.tramchester.livedata.EnrichPlatform;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.joda.time.LocalTime;
import org.junit.Before;
import org.junit.Test;

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
        RawWalkingStage rawWalkingStage = new RawWalkingStage(Stations.Altrincham, Stations.NavigationRoad, 15);
        TransportStage stage = new WalkingStage(rawWalkingStage, 8*60);

        StageDTO build = factory.build(stage);
        replayAll();
        checkValues(stage, build);
        verifyAll();
    }

    @Test
    public void shouldCreateStageDTOCorrectlyForTransportStage() {
        RawVehicleStage rawVehicleStage = new RawVehicleStage(Stations.MarketStreet, "routeName",
                TransportMode.Tram, "Displayclass");
        rawVehicleStage.setLastStation(Stations.Bury);
        rawVehicleStage.setCost(42);
        ServiceTime serviceTime = new ServiceTime(LocalTime.MIDNIGHT, LocalTime.MIDNIGHT.plusMinutes(5), "svcId",
                "headSign", "tripId");
        Platform platform = new Platform("platFormId", "platformName");
        rawVehicleStage.setPlatform(platform);
        TransportStage stage = new VehicleStageWithTiming(rawVehicleStage, serviceTime, TravelAction.Board);

        enrichPlatform.enrich(new PlatformDTO(platform));
        EasyMock.expectLastCall();

        replayAll();
        StageDTO stageDTO = factory.build(stage);
        verifyAll();

        checkValues(stage, stageDTO);
    }

    private void checkValues(TransportStage stage, StageDTO dto) {
        assertEquals(stage.getActionStation().getId(), dto.getActionStation().getId());
        assertEquals(stage.isWalk(), dto.isWalk());
        assertEquals(stage.getMode(), dto.getMode());
        assertEquals(stage.getFirstDepartureTime(), dto.getFirstDepartureTime());
        assertEquals(stage.getLastStation().getId(), dto.getLastStation().getId());
        assertEquals(stage.getExpectedArrivalTime(), dto.getExpectedArrivalTime());
        assertEquals(stage.getIsAVehicle(), dto.getIsAVehicle());
        assertEquals(stage.getDuration(), dto.getDuration());
        assertEquals(stage.getFirstStation().getId(), dto.getFirstStation().getId());
        assertEquals(stage.getHeadSign(), dto.getHeadSign());
        assertEquals(stage.getPrompt(), dto.getPrompt());
        assertEquals(stage.getSummary(), dto.getSummary());
        assertEquals(stage.getDisplayClass(), dto.getDisplayClass());
    }
}
