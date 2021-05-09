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
import org.opengis.referencing.operation.TransformException;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class StationLocationsTest extends EasyMockSupport {

    private StationLocations stationLocations;
    private CompositeStationRepository stationRepository;
    private TramchesterConfig config;

    @BeforeEach
    void onceBeforeEachTest() {
        stationRepository = createMock(CompositeStationRepository.class);
        config = createMock(TramchesterConfig.class);
        stationLocations = new StationLocations(stationRepository, config);
    }

    private void setStationExceptations(Station... stations) {
        Set<Station> toReturn = new HashSet<>(Arrays.asList(stations));
        EasyMock.expect(config.getTransportModes()).andReturn(Collections.singleton(TransportMode.Tram));
        EasyMock.expect(stationRepository.getStationsForMode(TransportMode.Tram)).andReturn(toReturn);
    }

    @Test
    void shouldHaveGridPositionBehaviours() {
        GridPosition gridPositionA = new GridPosition(3,4);
        assertEquals(3, gridPositionA.getEastings());
        assertEquals(4, gridPositionA.getNorthings());

        GridPosition origin = new GridPosition(0,0);
        assertEquals(5, GridPositions.distanceTo(origin, gridPositionA));
        assertEquals(5, GridPositions.distanceTo(gridPositionA, origin));

        assertFalse(GridPositions.withinDistEasting(origin, gridPositionA, 2));
        assertTrue(GridPositions.withinDistEasting(origin, gridPositionA, 3));
        assertTrue(GridPositions.withinDistEasting(origin, gridPositionA, 4));

        assertFalse(GridPositions.withinDistNorthing(origin, gridPositionA, 2));
        assertTrue(GridPositions.withinDistNorthing(origin, gridPositionA, 4));
        assertTrue(GridPositions.withinDistNorthing(origin, gridPositionA, 5));
    }

    @Test
    void shouldGetLatLongForStation() throws TransformException {
        Station stationA = createTestStation("id456", "nameB", TestEnv.nearPiccGardens);
        Station stationB = createTestStation("id789", "nameC", TestEnv.nearShudehill);
        setStationExceptations(stationA, stationB);

        LatLong resultA = stationA.getLatLong();
        LatLong resultB = stationB.getLatLong();

        replayAll();
        stationLocations.start();
        verifyAll();

        assertEquals(TestEnv.nearPiccGardens.getLat(), resultA.getLat(), 0.00001);
        assertEquals(TestEnv.nearPiccGardens.getLon(), resultA.getLon(), 0.00001);

        assertEquals(TestEnv.nearShudehill.getLat(), resultB.getLat(), 0.00001);
        assertEquals(TestEnv.nearShudehill.getLon(), resultB.getLon(), 0.00001);

    }

    @NotNull
    private Station createTestStation(String id, String name, LatLong location) throws TransformException {
        return TestStation.forTest(id, "area", name, location, TransportMode.Tram);
    }

    @Test
    void shouldFindNearbyStation() throws TransformException {
        LatLong place = TestEnv.nearAltrincham;
        Station stationA = createTestStation("id123", "nameA", place);
        Station stationB = createTestStation("id456", "nameB", TestEnv.nearPiccGardens);
        Station stationC = createTestStation("id789", "nameC", TestEnv.nearShudehill);
        LatLong closePlace = new LatLong(place.getLat()+0.008, place.getLon()+0.008);
        Station stationD = createTestStation("idABC", "name", closePlace);

        GridPosition gridA = stationA.getGridPosition();
        GridPosition gridB = stationD.getGridPosition();

        setStationExceptations(stationA, stationB, stationC, stationD);

        replayAll();
        stationLocations.start();
        verifyAll();

        int rangeInKM = 1;

        // validate within range on crude measure, but out of range on calculated position
        assertTrue(GridPositions.withinDistNorthing(gridA, gridB, 1000));
        assertTrue(GridPositions.withinDistEasting(gridA, gridB, 1000));
        long distance = GridPositions.distanceTo(gridA, gridB);
        assertTrue(distance > Math.round(rangeInKM*1000) );

        List<Station> results = stationLocations.nearestStationsSorted(place, 3, rangeInKM);

        assertEquals(1, results.size());
        assertEquals(stationA, results.get(0));
    }

    @Test
    void shouldOrderClosestFirst() throws TransformException {
        Station stationA = createTestStation("id123", "nameA", TestEnv.nearAltrincham);
        Station stationB = createTestStation("id456", "nameB", TestEnv.nearPiccGardens);
        Station stationC = createTestStation("id789", "nameC", TestEnv.nearShudehill);

        setStationExceptations(stationA, stationB, stationC);

        replayAll();
        stationLocations.start();
        List<Station> results = stationLocations.nearestStationsSorted(TestEnv.nearAltrincham, 3, 20);
        verifyAll();

        assertEquals(3, results.size());
        assertEquals(stationA, results.get(0));
        assertEquals(stationB, results.get(1));
        assertEquals(stationC, results.get(2));

    }

    @Test
    void shouldRespectLimitOnNumberResults() throws TransformException {
        Station stationA = createTestStation("id123", "nameA", TestEnv.nearAltrincham);
        Station stationB = createTestStation("id456", "nameB", TestEnv.nearPiccGardens);
        Station stationC = createTestStation("id789", "nameC", TestEnv.nearShudehill);

        setStationExceptations(stationA, stationB, stationC);

        replayAll();
        stationLocations.start();
        List<Station> results = stationLocations.nearestStationsSorted(TestEnv.nearAltrincham, 1, 20);
        verifyAll();

        assertEquals(1, results.size());
        assertEquals(stationA, results.get(0));
    }

    @Test
    void shouldFindNearbyStationRespectingRange() throws TransformException {
        Station testStation = createTestStation("id123", "name", TestEnv.nearAltrincham);
        setStationExceptations(testStation);

        replayAll();
        stationLocations.start();
        List<Station> results = stationLocations.nearestStationsSorted(TestEnv.nearPiccGardens, 3, 1);
        verifyAll();

        assertEquals(0, results.size());

        List<Station> further = stationLocations.nearestStationsSorted(TestEnv.nearPiccGardens, 3, 20);
        assertEquals(1, further.size());
        assertEquals(testStation, further.get(0));
    }

    @Test
    void shouldCaptureBoundingAreaForStations() throws TransformException {
        Station testStationA = createTestStation("id123", "name", TestEnv.nearAltrincham);
        Station testStationB = createTestStation("id456", "name", TestEnv.nearShudehill);
        Station testStationC = createTestStation("id789", "nameB", TestEnv.nearPiccGardens);

        setStationExceptations(testStationA, testStationB, testStationC);

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
    void shouldHaveExpectedBoundingBoxes() throws TransformException {
        long gridSize = 50;

        Station testStationA = createTestStation("id123", "name", TestEnv.nearPiccGardens);
        Station testStationC = createTestStation("id789", "nameB", TestEnv.nearShudehill);

        setStationExceptations(testStationA, testStationC);

        replayAll();
        stationLocations.start();
        BoundingBox area = stationLocations.getBounds();

        Set<BoundingBox> expected = new HashSet<>();
        for (long x = area.getMinEastings(); x <= area.getMaxEasting(); x = x + gridSize) {
            for (long y = area.getMinNorthings(); y <= area.getMaxNorthings(); y = y + gridSize) {
                BoundingBox box = new BoundingBox(x, y, x + gridSize, y + gridSize);
                expected.add(box);
            }
        }
        assertEquals(12, expected.size());

        Set<BoundingBox> found = stationLocations.getBoundingBoxsFor(gridSize).collect(Collectors.toSet());

        assertEquals(expected.size(), found.size());
        Set<BoundingBox> notFound = new HashSet<>(expected);
        notFound.removeAll(found);
        assertEquals(Collections.emptySet(), notFound);
    }

    @Test
    void shouldGridUpStations() throws TransformException {

        Station testStationA = createTestStation("id123", "name", TestEnv.nearAltrincham);
        Station testStationB = createTestStation("id456", "name", TestEnv.nearShudehill);
        Station testStationC = createTestStation("id789", "nameB", TestEnv.nearPiccGardens);

        setStationExceptations(testStationA, testStationB, testStationC);

        replayAll();
        stationLocations.start();
        List<BoundingBoxWithStations> boxedStations = stationLocations.getGroupedStations(1000).collect(Collectors.toList());
        verifyAll();

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
    }
}
