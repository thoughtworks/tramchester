package com.tramchester.unit.geo;

import com.tramchester.domain.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.StationLocations;
import com.tramchester.testSupport.TestEnv;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class StationLocationsTest {

    private StationLocations stationLocations;

    @Before
    public void onceBeforeEachTest() {
        stationLocations = new StationLocations();
    }

    @Test
    public void shouldConvertToGridCorrectly() {
        double lat = 52.940190;
        double lon = -1.4965572;

        LatLong latLong = new LatLong(lat, lon);
        Station testStation = new Station("id123", "area", "name", latLong, true);

        stationLocations.addStation(testStation);

        StationLocations.GridPosition result = stationLocations.getStationGridPosition(testStation);

        long expectedEasting = 433931;
        long expectedNorthing = 338207;

        assertEquals(expectedEasting, result.getEasting());
        assertEquals(expectedNorthing, result.getNorthing());
    }

    @Test
    public void shouldHaveGridPositionBehaviours() {
        StationLocations.GridPosition gridPositionA = new StationLocations.GridPosition(3,4);
        assertEquals(3, gridPositionA.getEasting());
        assertEquals(4, gridPositionA.getNorthing());

        StationLocations.GridPosition origin = new StationLocations.GridPosition(0,0);
        assertEquals(5, origin.distanceTo(gridPositionA));
        assertEquals(5, gridPositionA.distanceTo(origin));

        assertFalse(origin.withinDistEasting(gridPositionA, 2));
        assertTrue(origin.withinDistEasting(gridPositionA, 3));
        assertTrue(origin.withinDistEasting(gridPositionA, 4));

        assertFalse(origin.withinDistNorthing(gridPositionA, 2));
        assertTrue(origin.withinDistNorthing(gridPositionA, 4));
        assertTrue(origin.withinDistNorthing(gridPositionA, 5));

    }

    @Test
    public void shouldFindNearbyStation() {
        LatLong place = TestEnv.nearAltrincham;
        Station stationA = new Station("id123", "area", "nameA", place, true);
        Station stationB = new Station("id456", "area", "nameB", TestEnv.nearPiccGardens, true);
        Station stationC = new Station("id789", "area", "nameC", TestEnv.nearShudehill, true);
        LatLong closePlace = new LatLong(place.getLat()+0.008, place.getLon()+0.008);
        Station stationD = new Station("idABC", "area", "name", closePlace, true);

        stationLocations.addStation(stationA);
        stationLocations.addStation(stationB);
        stationLocations.addStation(stationC);
        stationLocations.addStation(stationD);

        StationLocations.GridPosition gridA = stationLocations.getStationGridPosition(stationA);
        StationLocations.GridPosition gridB = stationLocations.getStationGridPosition(stationD);

        int rangeInKM = 1;

        // validate within range on crude measure, but out of range on calculated position
        assertTrue(gridA.withinDistNorthing(gridB, 1000));
        assertTrue(gridA.withinDistEasting(gridB, 1000));
        long distance = gridA.distanceTo(gridB);
        assertTrue(distance > Math.round(rangeInKM*1000) );

        List<Station> results = stationLocations.nearestStations(place, 3, rangeInKM);

        assertEquals(1, results.size());
        assertEquals(stationA, results.get(0));
    }

    @Test
    public void shouldOrderClosestFirst() {
        Station stationA = new Station("id123", "area", "nameA", TestEnv.nearAltrincham, true);
        Station stationB = new Station("id456", "area", "nameB", TestEnv.nearPiccGardens, true);
        Station stationC = new Station("id789", "area", "nameC", TestEnv.nearShudehill, true);

        stationLocations.addStation(stationA);
        stationLocations.addStation(stationB);
        stationLocations.addStation(stationC);

        List<Station> results = stationLocations.nearestStations(TestEnv.nearAltrincham, 3, 20);
        assertEquals(3, results.size());
        assertEquals(stationA, results.get(0));
        assertEquals(stationB, results.get(1));
        assertEquals(stationC, results.get(2));
    }

    @Test
    public void shouldRespectLimitOnNumberResults() {
        Station stationA = new Station("id123", "area", "nameA", TestEnv.nearAltrincham, true);
        Station stationB = new Station("id456", "area", "nameB", TestEnv.nearPiccGardens, true);
        Station stationC = new Station("id789", "area", "nameC", TestEnv.nearShudehill, true);

        stationLocations.addStation(stationA);
        stationLocations.addStation(stationB);
        stationLocations.addStation(stationC);

        List<Station> results = stationLocations.nearestStations(TestEnv.nearAltrincham, 1, 20);
        assertEquals(1, results.size());
        assertEquals(stationA, results.get(0));
    }

    @Test
    public void shouldFindNearbyStationRespectingRange() {
        Station testStation = new Station("id123", "area", "name", TestEnv.nearAltrincham, true);
        stationLocations.addStation(testStation);

        List<Station> results = stationLocations.nearestStations(TestEnv.nearPiccGardens, 3, 1);
        assertEquals(0, results.size());

        List<Station> further = stationLocations.nearestStations(TestEnv.nearPiccGardens, 3, 20);
        assertEquals(1, further.size());
        assertEquals(testStation, further.get(0));

    }
}
