package com.tramchester.unit.repository;

import com.tramchester.domain.StationPair;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.repository.TramStationAdjacenyRepository;
import com.tramchester.testSupport.reference.TramTransportDataForTestProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StationAdjacencyRepositoryTest {

    private TramStationAdjacenyRepository repository;
    private TramTransportDataForTestProvider.TestTransportData transportDataSource;

    @BeforeEach
    void onceBeforeEachTestRuns() {
        ProvidesNow providesNow = new ProvidesLocalNow();
        transportDataSource = new TramTransportDataForTestProvider(providesNow).getTestData();
        repository = new TramStationAdjacenyRepository(transportDataSource);
        repository.start();
    }

    @Test
    void shouldGiveCorrectCostForAdjaceny() {
        Assertions.assertEquals(11, getAdjacent(transportDataSource.getFirst(), transportDataSource.getSecond()));
        Assertions.assertEquals(9, getAdjacent(transportDataSource.getSecond(), transportDataSource.getInterchange()));
        Assertions.assertEquals(-1, getAdjacent(transportDataSource.getFirst(), transportDataSource.getInterchange()));
    }

    int getAdjacent(Station first, Station second) {
        return repository.getAdjacent(StationPair.of(first, second));
    }
 }
