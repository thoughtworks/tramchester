package com.tramchester.unit.domain.presentation.DTO.factory;

import com.tramchester.domain.*;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.presentation.DTO.StageDTO;
import com.tramchester.domain.presentation.DTO.factory.StageDTOFactory;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.presentation.TravelAction;
import com.tramchester.domain.time.TramTime;
import com.tramchester.testSupport.Stations;
import com.tramchester.testSupport.TestEnv;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StageDTOFactoryTest extends EasyMockSupport {

    private StageDTOFactory factory;

    @BeforeEach
    void beforeEachTestRun() {
        factory = new StageDTOFactory();
    }

    @SuppressWarnings("JUnitTestMethodWithNoAssertions")
    @Test
    void shouldCreateStageDTOCorrectlyForWalking() {
        WalkingStage stage = new WalkingStage(Stations.Altrincham, Stations.NavigationRoad, 15,
                TramTime.of(8,11), false);

        StageDTO build = factory.build(stage, TravelAction.WalkTo);
        replayAll();
        checkValues(stage, build, false, TravelAction.WalkTo);
        verifyAll();
    }

    @Test
    void shouldCreateStageDTOCorrectlyForTransportStage() {
        Route testRoute = TestEnv.getTestRoute();
        Service service = new Service("svcId", testRoute);

        Trip trip = new Trip("tripId", "headSign", service, testRoute);
        VehicleStage vehicleStage = new VehicleStage(Stations.MarketStreet, testRoute,
                TransportMode.Tram, "Displayclass", trip, TramTime.of(0, 0), Stations.Bury, 23);
        vehicleStage.setCost(5);

        Platform platform = new Platform("platFormId", "platformName");
        vehicleStage.setPlatform(platform);

        replayAll();
        StageDTO stageDTO = factory.build(vehicleStage, TravelAction.Board);
        verifyAll();

        checkValues(vehicleStage, stageDTO, true, TravelAction.Board);
    }

    private void checkValues(TransportStage stage, StageDTO dto, boolean hasPlatform, TravelAction action) {
        Assertions.assertEquals(stage.getActionStation().getId(), dto.getActionStation().getId());
        Assertions.assertEquals(stage.getMode(), dto.getMode());
        Assertions.assertEquals(stage.getFirstDepartureTime(), dto.getFirstDepartureTime());
        Assertions.assertEquals(stage.getLastStation().getId(), dto.getLastStation().getId());
        Assertions.assertEquals(stage.getExpectedArrivalTime(), dto.getExpectedArrivalTime());
        Assertions.assertEquals(stage.getDuration(), dto.getDuration());
        Assertions.assertEquals(stage.getFirstStation().getId(), dto.getFirstStation().getId());
        Assertions.assertEquals(stage.getHeadSign(), dto.getHeadSign());
        Assertions.assertEquals(stage.getRouteName(), dto.getRouteName());
        Assertions.assertEquals(stage.getDisplayClass(), dto.getDisplayClass());
        Assertions.assertEquals(stage.getPassedStops(), dto.getPassedStops());
        Assertions.assertEquals(action.toString(), dto.getAction());
        Assertions.assertEquals(hasPlatform, dto.getHasPlatform());
    }
}
