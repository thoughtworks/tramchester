package com.tramchester.integration.repository;

import com.tramchester.Dependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.input.TramInterchanges;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.IntegrationBusTestConfig;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.testSupport.BusStations;
import com.tramchester.testSupport.BusTest;
import org.junit.experimental.categories.Category;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Disabled("WIP")
class BusInterchangeRepositoryTest {
    private static TramchesterConfig config;
    private static Dependencies dependencies;
    private InterchangeRepository repository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws IOException {
        dependencies = new Dependencies();
        dependencies.initialise(new IntegrationBusTestConfig());
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }
    @BeforeEach
    void onceBeforeEachTestRuns() {
        repository = dependencies.get(InterchangeRepository.class);
        config = dependencies.getConfig();
    }

    @Test
    void shouldFindTramInterchanges() {
        for (String interchange : TramInterchanges.stations()) {
            Assertions.assertTrue(repository.isInterchange(interchange));
        }
    }

    @Category({BusTest.class})
    @Test
    void shouldFindBusInterchanges() {
        Assumptions.assumeTrue(config.getBus());

        Collection<Station> interchanges = repository.getBusInterchanges();

        for (Station interchange : interchanges) {
            Assertions.assertFalse(interchange.isTram());
        }

        Assertions.assertFalse(interchanges.isEmpty());
        Set<String> interchangeIds = interchanges.stream().map(Station::getId).collect(Collectors.toSet());
        Assertions.assertTrue(interchangeIds.contains(BusStations.AltrinchamInterchange.getId()));
    }

}
