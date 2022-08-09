package com.tramchester.integration.repository;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.RouteEndRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.Summer2022;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteEndRepositoryTramTest {
    private static ComponentContainer componentContainer;
    private RouteEndRepository endStationsRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        componentContainer = new ComponentsBuilder().create(new IntegrationTramTestConfig(), TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        endStationsRepository = componentContainer.get(RouteEndRepository.class);
    }

    @Summer2022
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

        // TODO +5 again for additional replacement routes
        assertEquals(eolIds.size() + 5 + 5, results.size(), results.toString()); // +4 -> +5 TODO Summer 2022
    }

    @Test
    void shouldFindEndsOfLinesForTramSummer2021() {
        IdSet<Station> results = endStationsRepository.getStations(TransportMode.Tram);

        assertTrue(results.contains(Cornbrook.getId()));
        assertTrue(results.contains(Victoria.getId()));
        assertTrue(results.contains(Piccadilly.getId()));
        IdSet<Station> eolIds = EndOfTheLine.stream().map(TramStations::getId).collect(IdSet.idCollector());
        assertTrue(results.containsAll(eolIds));

//        assertEquals(eolIds.size()+3, results.size());
    }
}
