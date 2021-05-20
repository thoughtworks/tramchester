package com.tramchester.unit.geo;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.*;
import com.tramchester.repository.CompositeStationRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TestStation;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class StationLocationsTest extends EasyMockSupport {

    private StationLocations stationLocations;
    private StationRepository stationRepository;
    private CompositeStationRepository compositeStationRepository;

    @BeforeEach
    void onceBeforeEachTest() {
        stationRepository = createMock(StationRepository.class);
        compositeStationRepository = createMock(CompositeStationRepository.class);
        stationLocations = new StationLocations(stationRepository, compositeStationRepository);
    }

    @Test
    void shouldHaveGridPositionBehaviours() {
        GridPosition gridPositionA = new GridPosition(3,4);
        assertEquals(3, gridPositionA.getEastings());
        assertEquals(4, gridPositionA.getNorthings());

        GridPosition origin = new GridPosition(0,0);
        assertEquals(5, GridPositions.distanceTo(origin, gridPositionA));
        assertEquals(5, GridPositions.distanceTo(gridPositionA, origin));

        assertFalse(GridPositions.withinDistEasting(origin, gridPositionA, MarginInMeters.of(2)));
        assertTrue(GridPositions.withinDistEasting(origin, gridPositionA,  MarginInMeters.of(3)));
        assertTrue(GridPositions.withinDistEasting(origin, gridPositionA,  MarginInMeters.of(4)));

        assertFalse(GridPositions.withinDistNorthing(origin, gridPositionA,  MarginInMeters.of(2)));
        assertTrue(GridPositions.withinDistNorthing(origin, gridPositionA,  MarginInMeters.of(4)));
        assertTrue(GridPositions.withinDistNorthing(origin, gridPositionA,  MarginInMeters.of(5)));
    }

    @NotNull
    private Station createTestStation(String id, String name, LatLong location) {
        return TestStation.forTest(id, "area", name, location, TransportMode.Tram);
    }

    @Test
    void shouldFindNearbyStation() {
        MarginInMeters marginInMeters = MarginInMeters.of(1000);
        LatLong place = TestEnv.nearAltrincham;

        Station stationA = createTestStation("id123", "nameA", place);
        Station stationB = createTestStation("id456", "nameB", TestEnv.nearPiccGardens);
        Station stationC = createTestStation("id789", "nameC", TestEnv.nearShudehill);
        LatLong closePlace = new LatLong(place.getLat()+0.008, place.getLon()+0.008);
        Station stationD = createTestStation("idABC", "name", closePlace);

        GridPosition gridA = stationA.getGridPosition();
        GridPosition gridB = stationD.getGridPosition();

        //setStationExceptations(stationA, stationB, stationC, stationD);
        EasyMock.expect(stationRepository.getStationStream()).andReturn(Stream.empty());
        EasyMock.expect(compositeStationRepository.getStationStream()).andReturn(
                Stream.of(stationA, stationB, stationC, stationD));

        replayAll();
        stationLocations.start();
        List<Station> results = stationLocations.nearestStationsSorted(place, 3, marginInMeters);
        verifyAll();

        // validate within range on crude measure, but out of range on calculated position
        assertTrue(GridPositions.withinDistNorthing(gridA, gridB, marginInMeters));
        assertTrue(GridPositions.withinDistEasting(gridA, gridB, marginInMeters));
        long distance = GridPositions.distanceTo(gridA, gridB);
        assertTrue(distance > marginInMeters.get() );

        assertEquals(1, results.size());
        assertEquals(stationA, results.get(0));
    }

    @Test
    void shouldOrderClosestFirst() {
        Station stationA = createTestStation("id123", "nameA", TestEnv.nearAltrincham);
        Station stationB = createTestStation("id456", "nameB", TestEnv.nearPiccGardens);
        Station stationC = createTestStation("id789", "nameC", TestEnv.nearShudehill);

        EasyMock.expect(stationRepository.getStationStream()).andReturn(Stream.empty());
        EasyMock.expect(compositeStationRepository.getStationStream()).andReturn(Stream.of(stationA, stationB, stationC));

        replayAll();
        stationLocations.start();
        List<Station> results = stationLocations.nearestStationsSorted(TestEnv.nearAltrincham, 3,
                MarginInMeters.of(20000));
        verifyAll();

        assertEquals(3, results.size());
        assertEquals(stationA, results.get(0));
        assertEquals(stationB, results.get(1));
        assertEquals(stationC, results.get(2));

    }

    @Test
    void shouldRespectLimitOnNumberResults() {
        Station stationA = createTestStation("id123", "nameA", TestEnv.nearAltrincham);
        Station stationB = createTestStation("id456", "nameB", TestEnv.nearPiccGardens);
        Station stationC = createTestStation("id789", "nameC", TestEnv.nearShudehill);

        EasyMock.expect(stationRepository.getStationStream()).andReturn(Stream.empty());
        EasyMock.expect(compositeStationRepository.getStationStream()).andReturn(Stream.of(stationA, stationB, stationC));

        replayAll();
        stationLocations.start();
        List<Station> results = stationLocations.nearestStationsSorted(TestEnv.nearAltrincham, 1,
                MarginInMeters.of(20));
        verifyAll();

        assertEquals(1, results.size());
        assertEquals(stationA, results.get(0));
    }

    @Test
    void shouldFindNearbyStationRespectingRange() {
        Station testStation = createTestStation("id123", "name", TestEnv.nearAltrincham);

        EasyMock.expect(compositeStationRepository.getStationStream()).andReturn(Stream.of(testStation));
        EasyMock.expect(compositeStationRepository.getStationStream()).andReturn(Stream.of(testStation));

        replayAll();
        List<Station> results = stationLocations.nearestStationsSorted(TestEnv.nearPiccGardens, 3,
                MarginInMeters.of(1));
        List<Station> further = stationLocations.nearestStationsSorted(TestEnv.nearPiccGardens, 3,
                MarginInMeters.of(20000));
        verifyAll();

        assertEquals(0, results.size());
        assertEquals(1, further.size());
        assertEquals(testStation, further.get(0));
    }

    @Test
    void shouldCaptureBoundingAreaForStations() {
        Station testStationA = createTestStation("id123", "name", TestEnv.nearAltrincham);
        Station testStationB = createTestStation("id456", "name", TestEnv.nearShudehill);
        Station testStationC = createTestStation("id789", "nameB", TestEnv.nearPiccGardens);

        EasyMock.expect(stationRepository.getStationStream()).andReturn(Stream.of(testStationA, testStationB, testStationC));

        GridPosition posA = testStationA.getGridPosition();
        GridPosition posB = testStationB.getGridPosition();

        replayAll();
        stationLocations.start();
        BoundingBox bounds = stationLocations.getBounds();
        verifyAll();

        // bottom left
        assertEquals(posA.getEastings(), bounds.getMinEastings());
        assertEquals(posA.getNorthings(), bounds.getMinNorthings());
        // top right
        assertEquals(posB.getEastings(), bounds.getMaxEasting());
        assertEquals(posB.getNorthings(), bounds.getMaxNorthings());
    }

    @Test
    void shouldHaveExpectedBoundingBoxes() {
        long gridSize = 50;

        Station testStationA = createTestStation("id123", "name", TestEnv.nearPiccGardens);
        Station testStationC = createTestStation("id789", "nameB", TestEnv.nearShudehill);

        EasyMock.expect(stationRepository.getStationStream()).andStubAnswer(() -> Stream.of(testStationA, testStationC));

        replayAll();
        stationLocations.start();
        BoundingBox area = stationLocations.getBounds();
        Set<BoundingBox> found = stationLocations.getBoundingBoxsFor(gridSize).collect(Collectors.toSet());
        verifyAll();

        Set<BoundingBox> expected = new HashSet<>();
        for (long x = area.getMinEastings(); x <= area.getMaxEasting(); x = x + gridSize) {
            for (long y = area.getMinNorthings(); y <= area.getMaxNorthings(); y = y + gridSize) {
                BoundingBox box = new BoundingBox(x, y, x + gridSize, y + gridSize);
                expected.add(box);
            }
        }
        assertEquals(12, expected.size());

        assertEquals(expected.size(), found.size());
        Set<BoundingBox> notFound = new HashSet<>(expected);
        notFound.removeAll(found);
        assertEquals(Collections.emptySet(), notFound);
    }

    @Test
    void shouldGridUpStations() {

        Station testStationA = createTestStation("id123", "name", TestEnv.nearAltrincham);
        Station testStationB = createTestStation("id456", "name", TestEnv.nearShudehill);
        Station testStationC = createTestStation("id789", "nameB", TestEnv.nearPiccGardens);

        EasyMock.expect(stationRepository.getStationStream()).andStubAnswer(() -> Stream.of(testStationA, testStationB, testStationC));

        replayAll();
        stationLocations.start();
        List<BoundingBoxWithStations> boxedStations = stationLocations.getGroupedStations(1000).collect(Collectors.toList());

        assertEquals(2, boxedStations.size());

        // one box should contain the two central stations
        Optional<BoundingBoxWithStations> maybeCentral = boxedStations.stream().filter(box -> box.getStaions().size() == 2).findFirst();
        assertTrue(maybeCentral.isPresent());
        BoundingBoxWithStations centralBox = maybeCentral.get();
        Set<Station> central = centralBox.getStaions();
        assertEquals(2, central.size());
        assertTrue(central.containsAll(Arrays.asList(testStationB, testStationC)));
        assertTrue(centralBox.contained(CoordinateTransforms.getGridPosition(TestEnv.nearShudehill)));
        assertTrue(centralBox.contained(CoordinateTransforms.getGridPosition(TestEnv.nearPiccGardens)));

        // other box should contain the one non-central
        Optional<BoundingBoxWithStations> maybeAlty = boxedStations.stream().filter(box -> box.getStaions().size() == 1).findFirst();
        assertTrue(maybeAlty.isPresent());
        BoundingBoxWithStations altyBox = maybeAlty.get();
        Set<Station> alty = altyBox.getStaions();
        assertEquals(1, alty.size());
        assertTrue(alty.contains(testStationA));
        assertTrue(altyBox.contained(CoordinateTransforms.getGridPosition(TestEnv.nearAltrincham)));

        verifyAll();

    }

}
