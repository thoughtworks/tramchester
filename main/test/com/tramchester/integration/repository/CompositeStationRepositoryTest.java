package com.tramchester.integration.repository;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.TramWithPostcodesEnabled;
import com.tramchester.repository.CompositeStationRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TransportDataFactory;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static org.junit.jupiter.api.Assertions.*;

class CompositeStationRepositoryTest {

    private static GuiceContainerDependencies<TransportDataFactory> componentContainer;
    private CompositeStationRepository repository;
    private StationRepository fullRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        componentContainer = new ComponentsBuilder<>().create(new TramWithPostcodesEnabled(), TestEnv.NoopRegisterMetrics());
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
    void shouldHaveNoneForStationsWithPlatforms() {
        assertEquals(0,repository.getNumberOfComposites());
        assertTrue(repository.getCompositesFor(Tram).isEmpty());
    }

    @Test
    void shouldHaveSameResultsAsFullRepository() {
        fullRepository.getStationsForMode(Tram).forEach(original -> {
            assertTrue(repository.hasStationId(original.getId()));
            Station found = repository.getStationById(original.getId());
            assertNotNull(found);
            assertEquals(original, found);
        });

        assertEquals(fullRepository.getStationsForMode(Tram), repository.getStationsForMode(Tram));
    }
}
