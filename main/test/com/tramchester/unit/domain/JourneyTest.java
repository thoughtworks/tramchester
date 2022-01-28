package com.tramchester.unit.domain;

import com.tramchester.domain.*;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.MutableTrip;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.MyLocation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.domain.transportStages.VehicleStage;
import com.tramchester.domain.transportStages.WalkingFromStationStage;
import com.tramchester.domain.transportStages.WalkingToStationStage;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.StationHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.tramchester.domain.id.StringIdFor.createId;
import static com.tramchester.domain.reference.TransportMode.*;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

class JourneyTest {

    private TramTime queryTime;
    private List<Location<?>> path;
    private final Route route = TestEnv.getTramTestRoute();
    private Trip trip;
    private List<Integer> stopSequenceNumbers;
    private MyLocation myLocation;
    private final int requestedNumberChanges = 4;

    @BeforeEach
    void beforeEachTest() {
        Service service = MutableService.build(createId("svc123"));
        trip = MutableTrip.build(createId("trip897"), "headsign", service, route);
        queryTime = TramTime.of(9,16);
        path = Collections.emptyList();
        stopSequenceNumbers = Arrays.asList(10,11,12,13);
        myLocation = new MyLocation(TestEnv.nearWythenshaweHosp);
    }

    @Test
    void shouldHaveDirectTram() {
        List<TransportStage<?, ?>> stages = new ArrayList<>();

        final TramTime departureTime = queryTime.plusMinutes(10);
        stages.add(createVehicleStage(of(Altrincham), of(StPetersSquare), Tram, departureTime, 15));
        Journey journey = new Journey(departureTime, queryTime, departureTime.plusMinutes(15), stages, path, requestedNumberChanges);

        Set<TransportMode> result = journey.getTransportModes();

        assertEquals(1, result.size());
        assertTrue(result.contains(Tram));
        assertTrue(journey.isDirect());
        assertFalse(journey.firstStageIsWalk());

        assertEquals(Altrincham.getId(), journey.getBeginning().getId());
    }

    @Test
    void shouldHaveDirectWalkToStation() {
        List<TransportStage<?, ?>> stages = new ArrayList<>();

        final TramTime departureTime = queryTime.plusMinutes(10);

        stages.add(new WalkingToStationStage(myLocation, of(Bury), 42, departureTime));

        Journey journey = new Journey(departureTime, queryTime, departureTime.plusMinutes(15), stages, path, requestedNumberChanges);

        Set<TransportMode> result = journey.getTransportModes();

        assertEquals(1, result.size());
        assertTrue(result.contains(Walk));
        assertTrue(journey.isDirect());
        assertTrue(journey.firstStageIsWalk());

        assertEquals(myLocation.getId(), journey.getBeginning().getId());
    }

    @Test
    void shouldHaveDirectWalkFromStation() {
        List<TransportStage<?, ?>> stages = new ArrayList<>();

        final TramTime departureTime = queryTime.plusMinutes(10);

        stages.add(new WalkingFromStationStage(of(Bury), myLocation, 42, departureTime));

        Journey journey = new Journey(departureTime, queryTime, departureTime.plusMinutes(15), stages, path, requestedNumberChanges);

        Set<TransportMode> result = journey.getTransportModes();

        assertEquals(1, result.size());
        assertTrue(result.contains(Walk));
        assertTrue(journey.isDirect());
        assertTrue(journey.firstStageIsWalk());

        assertEquals(Bury.getId(), journey.getBeginning().getId());
    }

    @Test
    void shouldHaveWalkThenVehicle() {
        List<TransportStage<?, ?>> stages = new ArrayList<>();

        final TramTime departureTime = queryTime.plusMinutes(10);

        stages.add(new WalkingToStationStage(myLocation, of(Bury), 42, departureTime));
        stages.add(createVehicleStage(of(Bury), of(StPetersSquare), Tram, departureTime.plusMinutes(42), 13));

        Journey journey = new Journey(departureTime, queryTime, departureTime.plusMinutes(15), stages, path, requestedNumberChanges);

        Set<TransportMode> result = journey.getTransportModes();

        assertEquals(2, result.size());
        assertTrue(result.contains(Walk));
        assertTrue(result.contains(Tram));
        assertTrue(journey.isDirect());
        assertTrue(journey.firstStageIsWalk());

        assertEquals(Bury.getId(), journey.getBeginning().getId());
    }

    @Test
    void shouldHaveWalkThenVehicleAndVehicle() {
        List<TransportStage<?, ?>> stages = new ArrayList<>();

        final TramTime departureTime = queryTime.plusMinutes(10);

        stages.add(new WalkingToStationStage(myLocation, of(Bury), 42, departureTime));
        stages.add(createVehicleStage(of(Bury), of(StPetersSquare), Tram, departureTime.plusMinutes(42), 13));
        stages.add(createVehicleStage(of(Victoria), of(ManAirport), Tram, departureTime.plusMinutes(42), 13));

        Journey journey = new Journey(departureTime, queryTime, departureTime.plusMinutes(15), stages, path, requestedNumberChanges);

        assertFalse(journey.isDirect());
        assertEquals(Bury.getId(), journey.getBeginning().getId());
    }

    @Test
    void shouldHaveVehicleThenWalkFromStation() {
        List<TransportStage<?, ?>> stages = new ArrayList<>();

        final TramTime departureTime = queryTime.plusMinutes(10);

        stages.add(createVehicleStage(of(Altrincham), of(StPetersSquare), Tram, departureTime.plusMinutes(5), 13));
        stages.add(new WalkingFromStationStage(of(Bury), myLocation, 42, departureTime.plusMinutes(18)));

        Journey journey = new Journey(departureTime, queryTime, departureTime.plusMinutes(15), stages, path, requestedNumberChanges);

        Set<TransportMode> result = journey.getTransportModes();

        assertEquals(2, result.size());
        assertTrue(result.contains(Walk));
        assertTrue(result.contains(Tram));
        assertFalse(journey.isDirect());
        assertFalse(journey.firstStageIsWalk());

        assertEquals(Altrincham.getId(), journey.getBeginning().getId());
    }

    @Test
    void shouldHaveBusThenTrain() {
        List<TransportStage<?, ?>> stages = new ArrayList<>();
        final TramTime departureTimeA = queryTime.plusMinutes(10);

        stages.add(createVehicleStage(of(Altrincham), of(StPetersSquare), Bus, departureTimeA, 13));
        stages.add(createVehicleStage(of(StPetersSquare), of(Victoria), Train, departureTimeA.plusMinutes(14), 19));

        Journey journey = new Journey(queryTime.plusMinutes(5), queryTime, queryTime.plusMinutes(10), stages, path, requestedNumberChanges);

        Set<TransportMode> result = journey.getTransportModes();
        final boolean direct = journey.isDirect();

        assertEquals(2, result.size());
        assertTrue(result.contains(Bus));
        assertTrue(result.contains(Train));
        assertFalse(direct);

    }

    @Test
    void shouldHaveDepartAndArrivalTime() {
        List<TransportStage<?, ?>> stages = new ArrayList<>();
        Journey journey = new Journey(queryTime.plusMinutes(5), queryTime, queryTime.plusMinutes(10), stages, path, requestedNumberChanges);

        assertEquals(queryTime, journey.getQueryTime());
        assertEquals(queryTime.plusMinutes(5), journey.getDepartTime());
        assertEquals(queryTime.plusMinutes(10), journey.getArrivalTime());
    }

    @Test
    void shouldHaveCallingPlatformIds() {
        final TramTime departureTimeA = queryTime.plusMinutes(10);

        final Platform platform1 = MutablePlatform.buildForTFGMTram("platformId1", "platformNameA", TestEnv.nearAltrincham);
        final Station alty = StationHelper.forTest(Altrincham, platform1);

        final Platform platform2 = MutablePlatform.buildForTFGMTram("platformId2", "platformNameA", TestEnv.nearStPetersSquare);
        final Station stPeters = StationHelper.forTest(StPetersSquare, platform2);

        final VehicleStage stageA = createVehicleStage(alty, stPeters, Bus, departureTimeA, 13);
        stageA.setPlatform(platform1);

        final Station victoria = StationHelper.forTest(Victoria);
        final VehicleStage stageB = createVehicleStage(stPeters, victoria, Train, departureTimeA.plusMinutes(14), 19);
        stageB.setPlatform(platform2);

        final VehicleStage stageC = createVehicleStage(victoria, of(ManAirport), Train, departureTimeA.plusMinutes(30), 5);

        List<TransportStage<?, ?>> stages = Arrays.asList(stageA, stageB, stageC);

        Journey journey = new Journey(queryTime.plusMinutes(5), queryTime, queryTime.plusMinutes(10), stages, path, requestedNumberChanges);

        IdSet<Platform> result = journey.getCallingPlatformIds();
        assertEquals(2, result.size());
        assertTrue(result.contains(createId("platformId1")));
        assertTrue(result.contains(createId("platformId2")));
        assertFalse(journey.isDirect());
        assertFalse(journey.firstStageIsWalk());

    }

    private VehicleStage createVehicleStage(Station firstStation, Station lastStation, TransportMode mode, TramTime departTime, int cost) {
        VehicleStage stage = new VehicleStage(firstStation, route, mode, trip, departTime, lastStation, stopSequenceNumbers);
        stage.setCost(cost);
        return stage;
    }

}
