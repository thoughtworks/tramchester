package com.tramchester.unit.geo;

import com.tramchester.domain.places.Station;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.SortsPositions;
import com.tramchester.geo.StationLocations;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.unit.graph.TransportDataForTest;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SortsPositionsTest {

    @Test
    void shouldOrderCorrectly() {
        StationLocations stationLocations = new StationLocations(new CoordinateTransforms());

        TransportDataForTest dataForTest = new TransportDataForTest(stationLocations);

        dataForTest.start();

        Station stationA = dataForTest.getSecondStation();
        Station stationB = dataForTest.getInterchange();
        Station stationC = dataForTest.getFirst();

        Station stationD = dataForTest.getFifthStation();

        List<SortsPositions.HasStationId<Station>> stations = new ArrayList<>();
        stations.add(new StationFacade(stationA));
        stations.add(new StationFacade(stationC));
        stations.add(new StationFacade(stationB));

        SortsPositions sortsPositions = new SortsPositions(dataForTest);

        List<Station> resultsFromD = sortsPositions.sortedByNearTo(Collections.singletonList(stationD.getId()), stations);

        assertEquals(stationA, resultsFromD.get(0));
        assertEquals(stationB, resultsFromD.get(1));
        assertEquals(stationC, resultsFromD.get(2));

        List<Station> resultsFromA = sortsPositions.sortedByNearTo(Collections.singletonList(stationA.getId()), stations);

        assertEquals(stationA, resultsFromA.get(0));
        assertEquals(stationB, resultsFromA.get(1));
        assertEquals(stationC, resultsFromD.get(2));
    }

    private static class StationFacade implements SortsPositions.HasStationId<Station> {
        private final Station station;

        private StationFacade(Station station) {
            this.station = station;
        }

        @Override
        public String getStationId() {
            return station.getId();
        }

        @Override
        public Station getContained() {
            return station;
        }
    }
}
