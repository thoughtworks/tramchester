package com.tramchester.unit.domain.presentation.DTO.factory;

import com.tramchester.domain.*;
import com.tramchester.domain.exceptions.TramchesterException;
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
import org.junit.Before;
import org.junit.Test;

import java.time.LocalTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
        TransportStage stage = new WalkingStage(rawWalkingStage, TramTime.of(8,0));

        StageDTO build = factory.build(stage);
        replayAll();
        checkValues(stage, build, false);
        verifyAll();
    }

    @Test
    public void shouldCreateStageDTOCorrectlyForTransportStage() throws TramchesterException {
        RawVehicleStage rawVehicleStage = new RawVehicleStage(Stations.MarketStreet, "routeName",
                TransportMode.Tram, "Displayclass");
        rawVehicleStage.setLastStation(Stations.Bury,23);
        rawVehicleStage.setCost(42);
        ServiceTime serviceTime = new ServiceTime(TramTime.of(0, 0), TramTime.of(0,5), "svcId",
                "headSign", "tripId");
        Platform platform = new Platform("platFormId", "platformName");
        rawVehicleStage.setPlatform(platform);
        TransportStage stage = new VehicleStageWithTiming(rawVehicleStage, serviceTime, TravelAction.Board);

        enrichPlatform.enrich(new PlatformDTO(platform));
        EasyMock.expectLastCall();

        replayAll();
        StageDTO stageDTO = factory.build(stage);
        verifyAll();

        checkValues(stage, stageDTO, true);
    }

    private void checkValues(TransportStage stage, StageDTO dto, boolean hasPlatform) {
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
        assertEquals(stage.getAction().toString(), dto.getAction());
        assertEquals(hasPlatform, dto.getHasPlatform());
    }
}
