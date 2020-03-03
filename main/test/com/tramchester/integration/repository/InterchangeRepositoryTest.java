package com.tramchester.integration.repository;

import com.tramchester.Dependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Route;
import com.tramchester.domain.Station;
import com.tramchester.domain.input.TramInterchanges;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.testSupport.BusStations;
import com.tramchester.testSupport.BusTest;
import com.tramchester.testSupport.RouteCodesForTesting;
import com.tramchester.testSupport.TestConfig;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

@RunWith(Parameterized.class)
public class InterchangeRepositoryTest {
    private static TramchesterConfig config;
    private InterchangeRepository repository;

    @Parameterized.Parameters
    public static Iterable<? extends Object> data() throws IOException {
        return TestConfig.getDependencies();
    }

    @Parameterized.Parameter
    public Dependencies dependencies;

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        TestConfig.closeDependencies();
    }

    @Before
    public void onceBeforeEachTestRuns() {
        repository = dependencies.get(InterchangeRepository.class);
        config = dependencies.getConfig();
    }

    @Test
    public void shouldFindTramInterchanges() {
        for (String interchange : TramInterchanges.stations()) {
            assertTrue(repository.isInterchange(interchange));
        }
    }

    @Category({BusTest.class})
    @Test
    public void shouldFindBusInterchanges() {
        assumeTrue(config.getBus());

        Collection<Station> interchanges = repository.getBusInterchanges();

        for (Station interchange : interchanges) {
            assertFalse(interchange.isTram());
        }

        assertFalse(interchanges.isEmpty());
        Set<String> interchangeIds = interchanges.stream().map(Station::getId).collect(Collectors.toSet());
        assertTrue(interchangeIds.contains(BusStations.ALTRINCHAM_INTERCHANGE));
    }

}
