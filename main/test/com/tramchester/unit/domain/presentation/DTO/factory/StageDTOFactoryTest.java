package com.tramchester.unit.domain.presentation.DTO.factory;

import com.tramchester.domain.*;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.domain.presentation.DTO.StageDTO;
import com.tramchester.domain.presentation.DTO.StationDepartureInfoDTO;
import com.tramchester.domain.presentation.DTO.factory.StageDTOFactory;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.presentation.TravelAction;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.testSupport.Stations;
import com.tramchester.repository.LiveDataRepository;
import com.tramchester.testSupport.TestEnv;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.Assert.assertEquals;

class StageDTOFactoryTest extends EasyMockSupport {

    private StageDTOFactory factory;
    private LiveDataRepository liveDataRepository;
    private TramServiceDate tramServiceDate;

    @BeforeEach
    void beforeEachTestRun() {
        liveDataRepository = createMock(LiveDataRepository.class);
        factory = new StageDTOFactory(liveDataRepository);
        tramServiceDate = new TramServiceDate(TestEnv.LocalNow().toLocalDate());
    }

    @Test
    void shouldCreateStageDTOCorrectlyForWalking() {
        WalkingStage stage = new WalkingStage(Stations.Altrincham, Stations.NavigationRoad, 15,
                TramTime.of(8,11), false);

        StageDTO build = factory.build(stage, TravelAction.WalkTo, TramTime.of(8,0), tramServiceDate);
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

        Optional<StationDepartureInfo> maybeDepartInfo = Optional.empty(); // no live data
        EasyMock.expect(liveDataRepository.departuresFor(platform, tramServiceDate,  TramTime.of(8, 23))).
                andReturn(maybeDepartInfo);
        EasyMock.expectLastCall();

        replayAll();
        StageDTO stageDTO = factory.build(vehicleStage, TravelAction.Board, TramTime.of(8, 23), tramServiceDate);
        verifyAll();

        checkValues(vehicleStage, stageDTO, true, TravelAction.Board);
    }

    @Test
    void shouldCreateStageDTOCorrectlyForTransportStageHasLiveData() {
        Route testRoute = TestEnv.getTestRoute();
        Service service = new Service("svcId", testRoute);
        Trip trip = new Trip("tripId", "headSign", service, testRoute);
        VehicleStage vehicleStage = new VehicleStage(Stations.MarketStreet, testRoute,
                TransportMode.Tram, "Displayclass", trip, TramTime.of(0, 0), Stations.Bury, 23);
        vehicleStage.setCost(5);

        Platform platform = new Platform("platFormId", "platformName");
        vehicleStage.setPlatform(platform);

        LocalTime localTime = LocalTime.of(8, 23);
        TramTime queryTime = TramTime.of(localTime);

        LocalDateTime lastupdate = LocalDateTime.of(tramServiceDate.getDate(), localTime);
        StationDepartureInfo departInfo = new StationDepartureInfo("displayId", "lineName",
                StationDepartureInfo.Direction.Incoming, "platform", Stations.MarketStreet,
                "message", lastupdate);
        Optional<StationDepartureInfo> maybeDepartInfo = Optional.of(departInfo);
        EasyMock.expect(liveDataRepository.departuresFor(platform, tramServiceDate, queryTime)).
                andReturn(maybeDepartInfo);
        EasyMock.expectLastCall();

        replayAll();
        StageDTO stageDTO = factory.build(vehicleStage, TravelAction.Board, queryTime, tramServiceDate);
        verifyAll();

        StationDepartureInfoDTO departDTO = stageDTO.getPlatform().getStationDepartureInfo();
        Assertions.assertEquals("message", departDTO.getMessage());
        Assertions.assertEquals(Stations.MarketStreet.getName(), departDTO.getLocation());
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
