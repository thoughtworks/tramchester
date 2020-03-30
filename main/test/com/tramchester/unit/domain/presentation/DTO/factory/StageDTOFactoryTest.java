package com.tramchester.unit.domain.presentation.DTO.factory;

import com.tramchester.testSupport.TestConfig;
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
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.Assert.assertEquals;

public class StageDTOFactoryTest extends EasyMockSupport {

    private StageDTOFactory factory;
    private LiveDataRepository liveDataRepository;
    private TramServiceDate tramServiceDate;

    @Before
    public void beforeEachTestRun() {
        liveDataRepository = createMock(LiveDataRepository.class);
        factory = new StageDTOFactory(liveDataRepository);
        tramServiceDate = new TramServiceDate(TestConfig.LocalNow().toLocalDate());
    }

    @Test
    public void shouldCreateStageDTOCorrectlyForWalking() {
        WalkingStage stage = new WalkingStage(Stations.Altrincham, Stations.NavigationRoad, 15,
                TramTime.of(8,11), false);

        StageDTO build = factory.build(stage, TravelAction.WalkTo, TramTime.of(8,0), tramServiceDate);
        replayAll();
        checkValues(stage, build, false, TravelAction.WalkTo);
        verifyAll();
    }

    @Test
    public void shouldCreateStageDTOCorrectlyForTransportStage() {
        Service service = new Service("svcId", TestConfig.getTestRoute().getId());

        Trip trip = new Trip("tripId", "headSign", service, TestConfig.getTestRoute());
        VehicleStage vehicleStage = new VehicleStage(Stations.MarketStreet, TestConfig.getTestRoute(),
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
    public void shouldCreateStageDTOCorrectlyForTransportStageHasLiveData() {
        Service service = new Service("svcId", TestConfig.getTestRoute().getId());
        Trip trip = new Trip("tripId", "headSign", service, TestConfig.getTestRoute());
        VehicleStage vehicleStage = new VehicleStage(Stations.MarketStreet, TestConfig.getTestRoute(),
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
        assertEquals("message", departDTO.getMessage());
        assertEquals(Stations.MarketStreet.getName(), departDTO.getLocation());
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
