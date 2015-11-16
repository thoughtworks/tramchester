package com.tramchester.graph;

import com.tramchester.Dependencies;
import com.tramchester.config.TramchesterConfig;
import org.joda.time.DateTime;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;

public class GraphBuilderTestPerformance {

    private static Dependencies dependencies;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
    }

    @Before
    public void beforeEachTestRuns() {
        dependencies.get(RouteCalculator.class);
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Test
    public void shouldTestTimeToRebuildGraph() throws Exception {
        DateTime start = DateTime.now();

        dependencies.initialise(new PerformanceTestConfig());

        DateTime finished = DateTime.now();

        System.out.println("Initialisation took: " + finished.minus(start.getMillis()).getMillis());
    }


    private static class PerformanceTestConfig extends TramchesterConfig {
        @Override
        public boolean isRebuildGraph() {
            return true;
        }

        @Override
        public boolean isPullData() {
            return false;
        }

        @Override
        public boolean isFilterData() {
            return false;
        }

        @Override
        public String getGraphName() {
            return "perf_test_tramchester.db";
        }

        @Override
        public List<String> getClosedStations() {
            return asList("St Peters Square");
        }

        @Override
        public List<String> getAgencies() {
            return Arrays.asList("MET");
        }

        @Override
        public String getInstanceDataBaseURL() {
            return "http://localhost:8080";
        }

        @Override
        public String getTramDataUrl() {
            return "http://odata.tfgm.com/opendata/downloads/TfGMgtfs.zip";
        }
    }
}
