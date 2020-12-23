package com.tramchester.unit.domain.presentation.DTO.factory;

import com.tramchester.domain.*;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.MyLocation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.RouteRefDTO;
import com.tramchester.domain.presentation.DTO.StageDTO;
import com.tramchester.domain.presentation.DTO.factory.StageDTOFactory;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.presentation.TravelAction;
import com.tramchester.domain.reference.RouteDirection;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TestNoPlatformStation;
import com.tramchester.testSupport.reference.TramStations;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class StageDTOFactoryTest extends EasyMockSupport {

    private StageDTOFactory factory;
    private LocalDate when;
    private StationRepository stationRepository;

    @BeforeEach
    void beforeEachTestRun() {
        stationRepository = createMock(StationRepository.class);
        factory = new StageDTOFactory(stationRepository);
        when = TestEnv.testDay();
    }

    @SuppressWarnings("JUnitTestMethodWithNoAssertions")
    @Test
    void shouldCreateStageDTOCorrectlyForWalking() {
        MyLocation location = new MyLocation("nearAlty", TestEnv.nearAltrincham);
        WalkingFromStationStage stage = new WalkingFromStationStage(TramStations.of(Altrincham), location, 15,
                TramTime.of(8,11));

        StageDTO build = factory.build(stage, TravelAction.WalkTo, when);
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

        Platform platform = new Platform("platFormId", "platformName", new LatLong(1,1));
        vehicleStage.setPlatform(platform);

        replayAll();
        StageDTO stageDTO = factory.build(vehicleStage, TravelAction.Board, when);
        verifyAll();

        checkValues(vehicleStage, stageDTO, true, TravelAction.Board);
        assertEquals(trip.getId().forDTO(), stageDTO.getTripId());
    }

    @Test
    void shouldMapRouteNameForTrainRoutes() {
        Agency agency = new Agency("GW", "Great Western");
        Route route = new Route("routeId", "shortName", "GW train service from PAD to NBY", agency,
                TransportMode.Train, RouteDirection.Unknown);

        Service service = new Service("svcId", route);

        Trip trip = new Trip("tripId", "headSign", service, route);
        Station startStation = TestNoPlatformStation.forTest("idA", "areaA", "name A", TestEnv.nearStockportBus, TransportMode.Train);
        Station endStation = TestNoPlatformStation.forTest("idB", "areaB", "name B", TestEnv.nearPiccGardens, TransportMode.Train);
        VehicleStage vehicleStage = new VehicleStage(startStation, route,
                TransportMode.Train, trip, TramTime.of(0, 0), endStation, 23, false);
        vehicleStage.setCost(5);

        EasyMock.expect(stationRepository.hasStationId(IdFor.createId("NBY"))).andReturn(true);
        EasyMock.expect(stationRepository.getStationName(IdFor.createId("NBY"))).andReturn("Newbury");
        EasyMock.expect(stationRepository.hasStationId(IdFor.createId("PAD"))).andReturn(true);
        EasyMock.expect(stationRepository.getStationName(IdFor.createId("PAD"))).andReturn("London Paddington");

        replayAll();
        StageDTO stageDTO = factory.build(vehicleStage, TravelAction.Board, when);
        verifyAll();

        RouteRefDTO routeRefDTO = stageDTO.getRoute();

        assertEquals(route.getId().forDTO(), routeRefDTO.getId());
        assertEquals(route.getShortName(), routeRefDTO.getShortName());
        assertEquals(TransportMode.Train, routeRefDTO.getTransportMode());

        assertEquals("Great Western train service from London Paddington to Newbury", routeRefDTO.getRouteName());
    }

    private void checkValues(TransportStage<?,?> stage, StageDTO dto, boolean hasPlatform, TravelAction action) {
        assertEquals(stage.getActionStation().forDTO(), dto.getActionStation().getId());
        assertEquals(stage.getMode(), dto.getMode());
        assertEquals(stage.getFirstDepartureTime().toDate(when), dto.getFirstDepartureTime());
        assertEquals(stage.getLastStation().forDTO(), dto.getLastStation().getId());
        assertEquals(stage.getExpectedArrivalTime().toDate(when), dto.getExpectedArrivalTime());
        assertEquals(stage.getDuration(), dto.getDuration());
        assertEquals(stage.getFirstStation().forDTO(), dto.getFirstStation().getId());
        assertEquals(stage.getHeadSign(), dto.getHeadSign());
        assertEquals(stage.getRoute().getId().forDTO(), dto.getRoute().getId());
        assertEquals(stage.getPassedStops(), dto.getPassedStops());
        assertEquals(action.toString(), dto.getAction());
        assertEquals(hasPlatform, dto.getHasPlatform());
        assertEquals(when, dto.getQueryDate());
    }
}
