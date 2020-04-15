package com.tramchester.unit.repository;

import com.tramchester.geo.StationLocations;
import com.tramchester.repository.StationAdjacenyRepository;
import com.tramchester.unit.graph.TransportDataForTest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StationAdjacencyRepositoryTest {

    TransportDataForTest transportDataSource;
    private StationAdjacenyRepository repository;

    @Before
    public void onceBeforeEachTestRuns() {
        StationLocations stationLocations = new StationLocations();
        transportDataSource = new TransportDataForTest(stationLocations);
        repository = new StationAdjacenyRepository(transportDataSource);
    }

    @Test
    public void shouldGiveCorrectCostForAdjaceny() {
        assertEquals(11, repository.getAdjacent(transportDataSource.getFirst(), transportDataSource.getSecond()));
        assertEquals(9, repository.getAdjacent(transportDataSource.getSecond(), transportDataSource.getInterchange()));
        assertEquals(-1, repository.getAdjacent(transportDataSource.getFirst(), transportDataSource.getInterchange()));
    }
}
