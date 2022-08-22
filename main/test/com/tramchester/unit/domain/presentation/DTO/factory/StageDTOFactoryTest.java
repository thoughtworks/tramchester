package com.tramchester.unit.domain.presentation.DTO.factory;

import com.tramchester.domain.*;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.MutableTrip;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.MyLocation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.StageDTO;
import com.tramchester.domain.presentation.DTO.LocationRefWithPosition;
import com.tramchester.domain.presentation.DTO.factory.StageDTOFactory;
import com.tramchester.domain.presentation.DTO.factory.DTOFactory;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.presentation.TravelAction;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.domain.transportStages.VehicleStage;
import com.tramchester.domain.transportStages.WalkingFromStationStage;
import com.tramchester.testSupport.TestEnv;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static com.tramchester.domain.id.StringIdFor.createId;
import static com.tramchester.testSupport.reference.KnownLocations.nearAltrincham;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class StageDTOFactoryTest extends EasyMockSupport {

    private StageDTOFactory factory;
    private TramDate when;
    private DTOFactory stationDTOFactory;

    @BeforeEach
    void beforeEachTestRun() {
        stationDTOFactory = createMock(DTOFactory.class);
        factory = new StageDTOFactory(stationDTOFactory);
        when = TestEnv.testDay();
    }

    @SuppressWarnings("JUnitTestMethodWithNoAssertions")
    @Test
    void shouldCreateStageDTOCorrectlyForWalking() {
        MyLocation location = nearAltrincham.location();

        WalkingFromStationStage stage = new WalkingFromStationStage(Altrincham.fake(), location, Duration.ofMinutes(15),
                TramTime.of(8,11));

        EasyMock.expect(stationDTOFactory.createLocationRefWithPosition(Altrincham.fake())).
                andReturn(new LocationRefWithPosition(Altrincham.fake()));
        EasyMock.expect(stationDTOFactory.createLocationRefWithPosition(Altrincham.fake())).
                andReturn(new LocationRefWithPosition(Altrincham.fake()));
        EasyMock.expect(stationDTOFactory.createLocationRefWithPosition(location)).
                andReturn(new LocationRefWithPosition(location));

        replayAll();
        StageDTO build = factory.build(stage, TravelAction.WalkTo, when);
        checkValues(stage, build, false, TravelAction.WalkTo);
        verifyAll();
    }

    @Test
    void shouldCreateStageDTOCorrectlyForTransportStage() {
        Route testRoute = TestEnv.getTramTestRoute();
        Service service = MutableService.build(createId("svcId"));
        Trip trip = MutableTrip.build(createId("tripId"), "headSign", service, testRoute);

        List<Integer> stopCallIndexes = Arrays.asList(1,2,3,4);

        Station firstStation = MarketStreet.fakeWithPlatform("platFormId",
                new LatLong(1, 1), DataSourceID.unknown, IdFor.invalid());

        VehicleStage vehicleStage = new VehicleStage(firstStation, testRoute,
                TransportMode.Tram, trip, TramTime.of(0, 0), Bury.fake(),
                stopCallIndexes
        );
        vehicleStage.setCost(Duration.ofMinutes(5));

        vehicleStage.setPlatform(TestEnv.onlyPlatform(firstStation));

        EasyMock.expect(stationDTOFactory.createLocationRefWithPosition(firstStation)).
                andStubReturn(new LocationRefWithPosition(firstStation));
        EasyMock.expect(stationDTOFactory.createLocationRefWithPosition(Bury.fake())).
                andReturn(new LocationRefWithPosition(Bury.fake()));

        replayAll();
        StageDTO stageDTO = factory.build(vehicleStage, TravelAction.Board, when);
        verifyAll();

        checkValues(vehicleStage, stageDTO, true, TravelAction.Board);
        assertEquals(trip.getId().forDTO(), stageDTO.getTripId());
    }

    private void checkValues(TransportStage<?,?> stage, StageDTO dto, boolean hasPlatform, TravelAction action) {
        assertEquals(stage.getActionStation().forDTO(), dto.getActionStation().getId());
        assertEquals(stage.getMode(), dto.getMode());
        assertEquals(stage.getFirstDepartureTime().toDate(when), dto.getFirstDepartureTime());
        assertEquals(stage.getLastStation().forDTO(), dto.getLastStation().getId());
        assertEquals(stage.getExpectedArrivalTime().toDate(when), dto.getExpectedArrivalTime());
        assertEquals(stage.getDuration(), Duration.ofMinutes(dto.getDuration()));
        assertEquals(stage.getFirstStation().forDTO(), dto.getFirstStation().getId());
        assertEquals(stage.getHeadSign(), dto.getHeadSign());

        assertEquals(stage.getRoute().getName(), dto.getRoute().getRouteName());
        assertEquals(stage.getRoute().getShortName(), dto.getRoute().getShortName());

        assertEquals(stage.getPassedStopsCount(), dto.getPassedStops());
        assertEquals(action.toString(), dto.getAction());
        assertEquals(hasPlatform, dto.getHasPlatform());
        assertEquals(when.toLocalDate(), dto.getQueryDate());
    }
}
