package com.tramchester.unit.repository;

import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.geo.StationLocations;
import com.tramchester.repository.TramStationAdjacenyRepository;
import com.tramchester.testSupport.reference.TransportDataForTestProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StationAdjacencyRepositoryTest {

    private TramStationAdjacenyRepository repository;
    private TransportDataForTestProvider.TestTransportData transportDataSource;

    @BeforeEach
    void onceBeforeEachTestRuns() {
        ProvidesNow providesNow = new ProvidesLocalNow();
        transportDataSource = new TransportDataForTestProvider(providesNow).getTestData();
        repository = new TramStationAdjacenyRepository(transportDataSource);
        repository.start();
    }

    @Test
    void shouldGiveCorrectCostForAdjaceny() {
        Assertions.assertEquals(11, repository.getAdjacent(transportDataSource.getFirst(), transportDataSource.getSecond()));
        Assertions.assertEquals(9, repository.getAdjacent(transportDataSource.getSecond(), transportDataSource.getInterchange()));
        Assertions.assertEquals(-1, repository.getAdjacent(transportDataSource.getFirst(), transportDataSource.getInterchange()));
    }
}
