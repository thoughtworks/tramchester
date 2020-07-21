package com.tramchester.unit.repository;

import com.tramchester.geo.StationLocations;
import com.tramchester.repository.TramStationAdjacenyRepository;
import com.tramchester.unit.graph.TransportDataForTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StationAdjacencyRepositoryTest {

    private TransportDataForTest transportDataSource;
    private TramStationAdjacenyRepository repository;

    @BeforeEach
    void onceBeforeEachTestRuns() {
        StationLocations stationLocations = new StationLocations();
        transportDataSource = new TransportDataForTest(stationLocations);
        repository = new TramStationAdjacenyRepository(transportDataSource);
        transportDataSource.start();
        repository.start();
    }

    @Test
    void shouldGiveCorrectCostForAdjaceny() {
        Assertions.assertEquals(11, repository.getAdjacent(transportDataSource.getFirst(), transportDataSource.getSecond()));
        Assertions.assertEquals(9, repository.getAdjacent(transportDataSource.getSecond(), transportDataSource.getInterchange()));
        Assertions.assertEquals(-1, repository.getAdjacent(transportDataSource.getFirst(), transportDataSource.getInterchange()));
    }
}
