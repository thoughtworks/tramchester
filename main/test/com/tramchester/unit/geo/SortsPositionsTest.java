package com.tramchester.unit.geo;

import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.MutableStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.geo.SortsPositions;
import com.tramchester.testSupport.reference.StationHelper;
import com.tramchester.testSupport.reference.TramTransportDataForTestFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.tramchester.testSupport.reference.KnownLocations.nearAltrincham;
import static org.junit.jupiter.api.Assertions.*;

class SortsPositionsTest {

    private TramTransportDataForTestFactory.TramTransportDataForTest dataForTest;
    private Station stationNearShudehill;
    private Station stationNearAltrincham;
    private Station StationNearStockportBus;
    private Station startionNearWythenshawHosp;

    private MutableStation alsoNearAlty;

    @BeforeEach
    void beforeEachTestRuns() {
        ProvidesNow providesNow = new ProvidesLocalNow();

        TramTransportDataForTestFactory dataForTestProvider = new TramTransportDataForTestFactory(providesNow);
        dataForTestProvider.start();
        dataForTest = dataForTestProvider.getTestData();

        stationNearAltrincham = dataForTest.getFirst();
        startionNearWythenshawHosp = dataForTest.getSecond(); // near PiccGardens
        stationNearShudehill = dataForTest.getInterchange();   // near Shudehill
        StationNearStockportBus = dataForTest.getFifthStation();  // nearStockportBus

        alsoNearAlty = StationHelper.forTestMutable("ALSO1122", "area2", "alsoNearAltr", nearAltrincham, DataSourceID.tfgm);

        dataForTest.addStation(alsoNearAlty);
    }

    @Test
    void shouldOrderCorrectlySingleDest() {

        Set<SortsPositions.HasStationId<Station>> stations = new HashSet<>();
        stations.add(new StationFacade(startionNearWythenshawHosp));
        stations.add(new StationFacade(stationNearAltrincham));
        stations.add(new StationFacade(stationNearShudehill));
        stations.add(new StationFacade(alsoNearAlty));

        SortsPositions sortsPositions = new SortsPositions(dataForTest);

        List<Station> resultsFromStockport = sortsPositions.sortedByNearTo(IdSet.singleton(StationNearStockportBus.getId()), stations);
        assertEquals(stations.size(), resultsFromStockport.size());

        List<Station> bothNearAlty = Arrays.asList(stationNearAltrincham, alsoNearAlty); // same distance, ordering undefined

        //assertEquals(startionNearWythenshawHosp, resultsFromStockport.get(0));
        assertEquals(stationNearShudehill, resultsFromStockport.get(0));

        assertNotEquals(resultsFromStockport.get(3), resultsFromStockport.get(2));
        assertTrue(bothNearAlty.contains(resultsFromStockport.get(2)));
        assertTrue(bothNearAlty.contains(resultsFromStockport.get(3)));

        List<Station> resultsFromWythenshaw = sortsPositions.sortedByNearTo(IdSet.singleton(startionNearWythenshawHosp.getId()), stations);
        assertEquals(startionNearWythenshawHosp, resultsFromWythenshaw.get(0));
        assertTrue(bothNearAlty.contains(resultsFromWythenshaw.get(1)));
        assertTrue(bothNearAlty.contains(resultsFromWythenshaw.get(2)));
    }

    @Test
    void shouldOrderCorrectlyMultipleDests() {

        Set<SortsPositions.HasStationId<Station>> stations = new HashSet<>();
        stations.add(new StationFacade(startionNearWythenshawHosp));
        stations.add(new StationFacade(stationNearAltrincham));
        stations.add(new StationFacade(stationNearShudehill));
        stations.add(new StationFacade(alsoNearAlty));

        SortsPositions sortsPositions = new SortsPositions(dataForTest);

        IdSet<Station> places = new IdSet<>();
        places.add(stationNearAltrincham.getId());
        places.add(StationNearStockportBus.getId());

        List<Station> resultsFromDandA = sortsPositions.sortedByNearTo(places, stations);
        assertEquals(stations.size(), resultsFromDandA.size());

        List<Station> firstOrSecond = Arrays.asList(stationNearAltrincham, alsoNearAlty); // same distance, ordering undefined

        assertNotEquals(resultsFromDandA.get(0), resultsFromDandA.get(1));
        assertTrue(firstOrSecond.contains(resultsFromDandA.get(0)));
        assertTrue(firstOrSecond.contains(resultsFromDandA.get(1)));

        assertEquals(startionNearWythenshawHosp, resultsFromDandA.get(2));
        assertEquals(stationNearShudehill, resultsFromDandA.get(3));

    }

    @Test
    void shouldCalculateMidPointOf() {
        SortsPositions sortsPositions = new SortsPositions(dataForTest);
        LocationSet stations = new LocationSet();
        stations.add(startionNearWythenshawHosp);
        stations.add(stationNearAltrincham);
        stations.add(stationNearShudehill);

        LatLong result = sortsPositions.midPointFrom(stations);

        double expectedLat = (startionNearWythenshawHosp.getLatLong().getLat() +
                stationNearAltrincham.getLatLong().getLat() +
                stationNearShudehill.getLatLong().getLat()) / 3D;

        double expectedLon = (startionNearWythenshawHosp.getLatLong().getLon() +
                stationNearAltrincham.getLatLong().getLon() +
                stationNearShudehill.getLatLong().getLon()) / 3D;

        assertEquals(expectedLat, result.getLat());
        assertEquals(expectedLon, result.getLon());

    }

    private static class StationFacade implements SortsPositions.HasStationId<Station> {
        private final Station station;

        private StationFacade(Station station) {
            assertNotNull(station);
            this.station = station;
        }

        @Override
        public IdFor<Station> getStationId() {
            return station.getId();
        }

        @Override
        public Station getContained() {
            return station;
        }
    }
}
