package com.tramchester.integration.repository;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.IntegrationBusTestConfig;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.BusStations;
import com.tramchester.testSupport.BusTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.*;


@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class BusInterchangeRepositoryTest {
    private static ComponentContainer componentContainer;
    private InterchangeRepository repository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        componentContainer = new ComponentsBuilder<>().create(new IntegrationBusTestConfig(), TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        repository = componentContainer.get(InterchangeRepository.class);
    }

    @BusTest
    @Test
    void shouldFindBusInterchanges() {
        IdSet<Station> interchanges = repository.getInterchangesFor(TransportMode.Bus);
        assertFalse(interchanges.isEmpty());
        assertTrue(interchanges.contains(BusStations.AltrinchamInterchange.getId()));
    }

}
