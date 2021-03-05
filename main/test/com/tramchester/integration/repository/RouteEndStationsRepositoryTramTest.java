package com.tramchester.integration.repository;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.integration.testSupport.IntegrationTramTestConfig;
import com.tramchester.repository.RouteEndStationsRepository;
import com.tramchester.repository.RouteRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteEndStationsRepositoryTramTest {
    private static ComponentContainer componentContainer;
    private RouteEndStationsRepository endStationsRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        componentContainer = new ComponentsBuilder<>().create(new IntegrationTramTestConfig(), TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        endStationsRepository = componentContainer.get(RouteEndStationsRepository.class);
    }

    @Test
    void shouldFindEndsOfLinesForTram() {
        IdSet<Station> results = endStationsRepository.getStations(TransportMode.Tram);

        // not officially end of a route, but routes finish here when returned to depot
        assertTrue(results.contains(TraffordBar.getId()));

        assertTrue(results.contains(Cornbrook.getId()));
        assertTrue(results.contains(Victoria.getId()));
        assertTrue(results.contains(Piccadilly.getId()));
        IdSet<Station> eolIds = EndOfTheLine.stream().map(TramStations::getId).collect(IdSet.idCollector());
        assertTrue(results.containsAll(eolIds));

        assertEquals(eolIds.size()+4, results.size());
    }
}
