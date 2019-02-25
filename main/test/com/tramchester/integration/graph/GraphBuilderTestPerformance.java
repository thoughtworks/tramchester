package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.TestConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class GraphBuilderTestPerformance {

    private static Dependencies dependencies;

    @Before
    public void beforeEachTestRuns() {
        dependencies = new Dependencies();
    }

    @After
    public void afterEachTestRuns() {
        dependencies.close();
    }

    @Test
    @Ignore("for performance testing")
    public void shouldTestTimeToFileDataAndRebuildGraph() throws Exception {

        LocalTime start = LocalTime.now();
        dependencies.initialise(new PerformanceTestConfig());
        LocalTime finished = LocalTime.now();

        System.out.println("Initialisation took: " + finished.minusNanos(start.getNano()).getNano());
    }

    private static class PerformanceTestConfig extends TestConfig {

        @Override
        public String getGraphName() {
            return "perf_test_tramchester.db";
        }

        @Override
        public Set<String> getAgencies() {

            return new HashSet<>(Arrays.asList("MET")); // , "GMS", "GMN"));
        }

        @Override
        public boolean getCreateLocality() {
            return false;
        }

        @Override
        public Path getDataFolder() {
            return Paths.get("data/all");
        }

    }
}
