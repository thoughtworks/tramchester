package com.tramchester.unit.geo;

import com.tramchester.domain.TransportMode;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.*;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TestStation;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opengis.referencing.operation.TransformException;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class StationLocationsTest {

    private StationLocations stationLocations;

    @BeforeEach
    void onceBeforeEachTest() {
        stationLocations = new StationLocations();
    }

    @Test
    void shouldHaveGridPositionBehaviours() {
        HasGridPosition gridPositionA = new GridPosition(3,4);
        assertEquals(3, gridPositionA.getEastings());
        assertEquals(4, gridPositionA.getNorthings());

        HasGridPosition origin = new GridPosition(0,0);
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
        stationLocations.addStation(stationA);
        stationLocations.addStation(stationB);

        LatLong resultA = stationLocations.getStationPosition(stationA);
        assertEquals(TestEnv.nearPiccGardens.getLat(), resultA.getLat(), 0.00001);
        assertEquals(TestEnv.nearPiccGardens.getLon(), resultA.getLon(), 0.00001);

        LatLong resultB = stationLocations.getStationPosition(stationB);
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

        stationLocations.addStation(stationA);
        stationLocations.addStation(stationB);
        stationLocations.addStation(stationC);
        stationLocations.addStation(stationD);

        HasGridPosition gridA = stationLocations.getStationGridPosition(stationA);
        HasGridPosition gridB = stationLocations.getStationGridPosition(stationD);

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

        stationLocations.addStation(stationA);
        stationLocations.addStation(stationB);
        stationLocations.addStation(stationC);

        List<Station> results = stationLocations.nearestStationsSorted(TestEnv.nearAltrincham, 3, 20);
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

        stationLocations.addStation(stationA);
        stationLocations.addStation(stationB);
        stationLocations.addStation(stationC);

        List<Station> results = stationLocations.nearestStationsSorted(TestEnv.nearAltrincham, 1, 20);
        assertEquals(1, results.size());
        assertEquals(stationA, results.get(0));
    }

    @Test
    void shouldFindNearbyStationRespectingRange() throws TransformException {
        Station testStation = createTestStation("id123", "name", TestEnv.nearAltrincham);
        stationLocations.addStation(testStation);

        List<Station> results = stationLocations.nearestStationsSorted(TestEnv.nearPiccGardens, 3, 1);
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

        stationLocations.addStation(testStationA);
        stationLocations.addStation(testStationB);
        stationLocations.addStation(testStationC);

        HasGridPosition posA = stationLocations.getStationGridPosition(testStationA);
        HasGridPosition posB = stationLocations.getStationGridPosition(testStationB);

        BoundingBox bounds = stationLocations.getBounds();

        // bottom left
        assertEquals(posA.getEastings(), bounds.getMinEastings());
        assertEquals(posA.getNorthings(), bounds.getMinNorthings());
        // top right
        assertEquals(posB.getEastings(), bounds.getMaxEasting());
        assertEquals(posB.getNorthings(), bounds.getMaxNorthings());
    }

    @Test
    void shouldGridUpStations() throws TransformException {

        Station testStationA = createTestStation("id123", "name", TestEnv.nearAltrincham);
        Station testStationB = createTestStation("id456", "name", TestEnv.nearShudehill);
        Station testStationC = createTestStation("id789", "nameB", TestEnv.nearPiccGardens);

        stationLocations.addStation(testStationA);
        stationLocations.addStation(testStationB);
        stationLocations.addStation(testStationC);

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
    }
}
