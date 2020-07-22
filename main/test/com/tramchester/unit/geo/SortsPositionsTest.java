package com.tramchester.unit.geo;

import com.tramchester.domain.IdFor;
import com.tramchester.domain.IdSet;
import com.tramchester.domain.TransportMode;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.SortsPositions;
import com.tramchester.geo.StationLocations;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TransportDataForTestFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SortsPositionsTest {

    private TransportDataForTestFactory.TestTransportData dataForTest;
    private Station nearPiccGardens;
    private Station nearShudehill;
    private Station nearAltrincham;
    private Station nearStockportBus;
    private Station alsoNearAlty;

    @BeforeEach
    void beforeEachTestRuns() {
        StationLocations stationLocations = new StationLocations();

        dataForTest = new TransportDataForTestFactory(stationLocations).get();
//        dataForTest.start();

        nearPiccGardens = dataForTest.getSecond(); // near PiccGardens
        nearShudehill = dataForTest.getInterchange();   // near Shudehill
        nearAltrincham = dataForTest.getFirst();         // near Altrincham
        nearStockportBus = dataForTest.getFifthStation();  // nearStockportBus

        alsoNearAlty = Station.forTest("ALSO1122", "area2", "alsoNearAltr", TestEnv.nearAltrincham, TransportMode.Tram);
        dataForTest.addStation(alsoNearAlty);
    }

    @Test
    void shouldOrderCorrectlySingleDest() {

        Set<SortsPositions.HasStationId<Station>> stations = new HashSet<>();
        stations.add(new StationFacade(nearPiccGardens));
        stations.add(new StationFacade(nearAltrincham));
        stations.add(new StationFacade(nearShudehill));
        stations.add(new StationFacade(alsoNearAlty));

        SortsPositions sortsPositions = new SortsPositions(dataForTest);

        List<Station> resultsFromD = sortsPositions.sortedByNearTo(IdSet.singleton(nearStockportBus.getId()), stations);
        assertEquals(stations.size(), resultsFromD.size());

        List<Station> thirdOrFourth = Arrays.asList(nearAltrincham, alsoNearAlty); // same distance, ordering undefined

        assertEquals(nearPiccGardens, resultsFromD.get(0));
        assertEquals(nearShudehill, resultsFromD.get(1));

        assertNotEquals(resultsFromD.get(3), resultsFromD.get(2));
        assertTrue(thirdOrFourth.contains(resultsFromD.get(2)));
        assertTrue(thirdOrFourth.contains(resultsFromD.get(3)));

        List<Station> resultsFromA = sortsPositions.sortedByNearTo(IdSet.singleton(nearPiccGardens.getId()), stations);

        assertEquals(nearPiccGardens, resultsFromA.get(0));
        assertEquals(nearShudehill, resultsFromA.get(1));

        assertNotEquals(resultsFromD.get(3), resultsFromD.get(2));
        assertTrue(thirdOrFourth.contains(resultsFromD.get(2)));
        assertTrue(thirdOrFourth.contains(resultsFromD.get(3)));

    }

    @Test
    void shouldOrderCorrectlyMultipleDests() {

        Set<SortsPositions.HasStationId<Station>> stations = new HashSet<>();
        stations.add(new StationFacade(nearPiccGardens));
        stations.add(new StationFacade(nearAltrincham));
        stations.add(new StationFacade(nearShudehill));
        stations.add(new StationFacade(alsoNearAlty));

        SortsPositions sortsPositions = new SortsPositions(dataForTest);

        IdSet<Station> places = new IdSet<>();
        places.add(nearAltrincham.getId());
        places.add(nearStockportBus.getId());

        List<Station> resultsFromDandA = sortsPositions.sortedByNearTo(places, stations);
        assertEquals(stations.size(), resultsFromDandA.size());

        List<Station> firstOrSecond = Arrays.asList(nearAltrincham, alsoNearAlty); // same distance, ordering undefined

        assertNotEquals(resultsFromDandA.get(0), resultsFromDandA.get(1));
        assertTrue(firstOrSecond.contains(resultsFromDandA.get(0)));
        assertTrue(firstOrSecond.contains(resultsFromDandA.get(1)));

        assertEquals(nearPiccGardens, resultsFromDandA.get(2));
        assertEquals(nearShudehill, resultsFromDandA.get(3));

    }

    @Test
    void shouldCalculateMidPointOf() {
        SortsPositions sortsPositions = new SortsPositions(dataForTest);
        Set<Station> stations = new HashSet<>();
        stations.add(nearPiccGardens);
        stations.add(nearAltrincham);
        stations.add(nearShudehill);

        LatLong result = sortsPositions.midPointFrom(stations);

        double expectedLat = (nearPiccGardens.getLatLong().getLat() +
                nearAltrincham.getLatLong().getLat() +
                nearShudehill.getLatLong().getLat()) / 3D;

        double expectedLon = (nearPiccGardens.getLatLong().getLon() +
                nearAltrincham.getLatLong().getLon() +
                nearShudehill.getLatLong().getLon()) / 3D;

        assertEquals(expectedLat, result.getLat());
        assertEquals(expectedLon, result.getLon());

    }

    private static class StationFacade implements SortsPositions.HasStationId<Station> {
        private final Station station;

        private StationFacade(Station station) {
            this.station = station;
        }

        @Override
        public IdFor<Station> getId() {
            return station.getId();
        }

        @Override
        public Station getContained() {
            return station;
        }
    }
}
