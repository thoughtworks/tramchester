package com.tramchester.integration.repository;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.StationAvailabilityRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.tramchester.domain.time.TramTime.of;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StationAvailabilityRepositoryTest {
    private static ComponentContainer componentContainer;

    private StationAvailabilityRepository repository;
    private StationRepository stationRepository;

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
        stationRepository = componentContainer.get(StationRepository.class);
        repository = componentContainer.get(StationAvailabilityRepository.class);
    }

    @Test
    void shouldBeAvailableAtExpectedHours() {

        Station stPeters = TramStations.StPetersSquare.from(stationRepository);

        TramDate when = TestEnv.testTramDay();

        boolean duringTheDay = repository.isAvailable(stPeters, when, TimeRange.of(of(8,45), of(10,45)));

        assertTrue(duringTheDay);

        boolean lateAtNight = repository.isAvailable(stPeters, when, TimeRange.of(of(3,5), of(3,15)));

        assertFalse(lateAtNight);
    }

}
