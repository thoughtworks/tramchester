package com.tramchester.integration.repository;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.tram.TramWithPostcodesEnabled;
import com.tramchester.repository.CompositeStationRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static org.junit.jupiter.api.Assertions.*;

class GroupedStationsRepositoryTest {

    private static GuiceContainerDependencies componentContainer;
    private CompositeStationRepository repository;
    private StationRepository fullRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        final TramWithPostcodesEnabled config = new TramWithPostcodesEnabled();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        repository = componentContainer.get(CompositeStationRepository.class);
        fullRepository = componentContainer.get(StationRepository.class);
    }

    @Test
    void shouldHaveNoneForTramStationsAsNoDuplicatedNames() {
        assertEquals(0, repository.getNumberOfComposites());
        assertTrue(repository.getCompositesServing(Tram).isEmpty());
    }

    @Test
    void shouldHaveSameResultsAsFullRepository() {
        fullRepository.getStationsServing(Tram).forEach(original -> {
            assertTrue(repository.hasStationId(original.getId()));
            Station found = repository.getStationById(original.getId());
            assertNotNull(found);
            assertEquals(original, found);
        });

        assertEquals(fullRepository.getStationsServing(Tram), repository.getStationsServing(Tram));
    }
}
