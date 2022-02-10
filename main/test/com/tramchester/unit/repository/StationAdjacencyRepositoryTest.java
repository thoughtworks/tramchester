package com.tramchester.unit.repository;

import com.tramchester.domain.StationPair;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.graph.filters.GraphFilterActive;
import com.tramchester.repository.TramStationAdjacenyRepository;
import com.tramchester.testSupport.reference.TramTransportDataForTestFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static com.tramchester.testSupport.TestEnv.assertMinutesEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StationAdjacencyRepositoryTest {

    private TramStationAdjacenyRepository repository;
    private TramTransportDataForTestFactory.TramTransportDataForTest transportDataSource;

    @BeforeEach
    void onceBeforeEachTestRuns() {
        ProvidesNow providesNow = new ProvidesLocalNow();

        TramTransportDataForTestFactory dataForTestProvider = new TramTransportDataForTestFactory(providesNow);
        dataForTestProvider.start();
        transportDataSource = dataForTestProvider.getTestData();

        repository = new TramStationAdjacenyRepository(transportDataSource, new GraphFilterActive(false));
        repository.start();
    }

    @Test
    void shouldGiveCorrectCostForAdjaceny() {
        assertMinutesEquals(11, getAdjacent(transportDataSource.getFirst(), transportDataSource.getSecond()));
        assertMinutesEquals(9, getAdjacent(transportDataSource.getSecond(), transportDataSource.getInterchange()));

        assertTrue(getAdjacent(transportDataSource.getFirst(), transportDataSource.getInterchange()).isNegative());
    }

    private Duration getAdjacent(Station first, Station second) {
        return repository.getAdjacent(StationPair.of(first, second));
    }
 }
