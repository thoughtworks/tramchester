package com.tramchester.integration.repository;

import com.tramchester.Dependencies;
import com.tramchester.domain.Station;
import com.tramchester.integration.BusTest;
import com.tramchester.integration.IntegrationBusTestConfig;
import com.tramchester.integration.IntegrationTramTestConfig;
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
    private TransportDataSource dataSource;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        dependencies.initialise(new IntegrationBusTestConfig("int_test_bus_tramchester.db"));
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
    public void shouldTestSomething() {
        InterchangeRepository repository = new InterchangeRepository(dataSource);

        List<Station> interchanges = repository.findAgencyInterchanges();

        assertFalse(interchanges.isEmpty());
    }

}
