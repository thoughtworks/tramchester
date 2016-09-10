package com.tramchester.graph;

import com.tramchester.Dependencies;
import com.tramchester.services.StationLocalityService;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class StopLocalityTest {

    private static Dependencies dependencies;
    private StationLocalityService service;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws Exception {
//        dependencies = new Dependencies();
//        dependencies.initialise(new IntegrationBusTestConfig());
    }

    @Before
    public void beforeEachTestRuns() {
        service = dependencies.get(StationLocalityService.class);
    }

    // Work in progress
    @Ignore
    @Test
    public void shouldTestSomething() {
        service.populateLocality();
    }


}
