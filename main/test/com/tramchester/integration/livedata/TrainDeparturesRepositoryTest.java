package com.tramchester.integration.livedata;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.RailAndTramGreaterManchesterConfig;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;
import com.tramchester.livedata.openLdb.TrainDeparturesRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.GMTest;
import com.tramchester.testSupport.testTags.TrainTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@GMTest
@TrainTest
public class TrainDeparturesRepositoryTest {

    private static GuiceContainerDependencies componentContainer;
    private TrainDeparturesRepository trainDeparturesRepository;
    private StationRepository stationRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        componentContainer = new ComponentsBuilder().create(new RailAndTramGreaterManchesterConfig(),
                TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforeEachTest() {
        stationRepository = componentContainer.get(StationRepository.class);
        trainDeparturesRepository = componentContainer.get(TrainDeparturesRepository.class);
    }

    @Test
    void shouldGetDeparturesForManchester() {

        // If only load stations within geo bounds then destinations for departures can be missing since they
        // can be outside of that area. This is a test for that, can all destinations be found.
        // TODO add test for known outside of area station as well?

        Station station = RailStationIds.ManchesterPiccadilly.from(stationRepository);

        List<UpcomingDeparture> departures = trainDeparturesRepository.forStation(station);

        assertFalse(departures.isEmpty());

        departures.forEach(departure -> {
            assertEquals(station, departure.getDisplayLocation());
            Station dest = departure.getDestination();
            assertNotNull(dest);
            assertTrue(station.getId().isValid(), "did not find destination " + dest);
        });
    }
}
