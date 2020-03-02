package com.tramchester.integration.repository;

import com.tramchester.Dependencies;
import com.tramchester.domain.Station;
import com.tramchester.testSupport.BusTest;
import com.tramchester.integration.IntegrationBusTestConfig;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.TransportDataFromFiles;
import com.tramchester.repository.TransportDataSource;
import org.junit.*;
import org.junit.experimental.categories.Category;

import java.util.List;

import static org.junit.Assert.assertFalse;

@Ignore("Experimental")
public class InterchangeRepositoryTest {
    private static Dependencies dependencies;
    private static IntegrationBusTestConfig config;
    private TransportDataSource dataSource;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        config = new IntegrationBusTestConfig("int_test_bus_tramchester.db");
        dependencies.initialise(config);
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Before
    public void onceBeforeEachTestRuns() {
        dataSource = dependencies.get(TransportDataFromFiles.class);
    }

    @Category({BusTest.class})
    @Test
    public void shouldFindBusInterchanges() {
        InterchangeRepository repository = new InterchangeRepository(dataSource, config);

        List<Station> interchanges = repository.getBusInterchanges();

        for (Station interchange : interchanges) {
            assertFalse(interchange.isTram());
        }

        assertFalse(interchanges.isEmpty());
    }

}
