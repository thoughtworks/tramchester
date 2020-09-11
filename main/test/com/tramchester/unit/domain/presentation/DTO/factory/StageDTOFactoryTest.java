package com.tramchester.unit.domain.presentation.DTO.factory;

import com.tramchester.domain.*;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.MyLocation;
import com.tramchester.domain.presentation.DTO.StageDTO;
import com.tramchester.domain.presentation.DTO.factory.StageDTOFactory;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.presentation.TravelAction;
import com.tramchester.domain.time.TramTime;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramStations;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.tramchester.testSupport.TramStations.*;

class StageDTOFactoryTest extends EasyMockSupport {

    private StageDTOFactory factory;

    @BeforeEach
    void beforeEachTestRun() {
        factory = new StageDTOFactory();
    }

    @SuppressWarnings("JUnitTestMethodWithNoAssertions")
    @Test
    void shouldCreateStageDTOCorrectlyForWalking() {
        MyLocation location = new MyLocation("nearAlty", TestEnv.nearAltrincham);
        WalkingFromStationStage stage = new WalkingFromStationStage(TramStations.of(Altrincham), location, 15,
                TramTime.of(8,11));

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
        VehicleStage vehicleStage = new VehicleStage(TramStations.of(MarketStreet), testRoute,
                TransportMode.Tram, trip, TramTime.of(0, 0), TramStations.of(Bury), 23, true);
        vehicleStage.setCost(5);

        Platform platform = new Platform("platFormId", "platformName");
        vehicleStage.setPlatform(platform);

        replayAll();
        StageDTO stageDTO = factory.build(vehicleStage, TravelAction.Board);
        verifyAll();

        checkValues(vehicleStage, stageDTO, true, TravelAction.Board);
    }

    private void checkValues(TransportStage<?,?> stage, StageDTO dto, boolean hasPlatform, TravelAction action) {
        Assertions.assertEquals(stage.getActionStation().forDTO(), dto.getActionStation().getId());
        Assertions.assertEquals(stage.getMode(), dto.getMode());
        Assertions.assertEquals(stage.getFirstDepartureTime(), dto.getFirstDepartureTime());
        Assertions.assertEquals(stage.getLastStation().forDTO(), dto.getLastStation().getId());
        Assertions.assertEquals(stage.getExpectedArrivalTime(), dto.getExpectedArrivalTime());
        Assertions.assertEquals(stage.getDuration(), dto.getDuration());
        Assertions.assertEquals(stage.getFirstStation().forDTO(), dto.getFirstStation().getId());
        Assertions.assertEquals(stage.getHeadSign(), dto.getHeadSign());
        Assertions.assertEquals(stage.getRouteName(), dto.getRouteName());
        Assertions.assertEquals(stage.getPassedStops(), dto.getPassedStops());
        Assertions.assertEquals(action.toString(), dto.getAction());
        Assertions.assertEquals(hasPlatform, dto.getHasPlatform());
    }
}
