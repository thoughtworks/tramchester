package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.testSupport.TestConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Disabled("for performance testing")
class GraphBuilderTestPerformance {

    private static Dependencies dependencies;

    @BeforeEach
    void beforeEachTestRuns() {
        dependencies = new Dependencies();
    }

    @AfterEach
    void afterEachTestRuns() {
        dependencies.close();
    }

    @Test
    void shouldTestTimeToFileDataAndRebuildGraph() throws Exception {

        long start = System.currentTimeMillis();
        dependencies.initialise(new PerformanceTestConfig());
        long duration = System.currentTimeMillis() - start;

        System.out.println("Initialisation took: " + duration + "ms");
    }

    private static class PerformanceTestConfig extends TestConfig {

        @Override
        public String getGraphName() {
            return "perf_test_tramchester.db";
        }

        @Override
        public Set<String> getAgencies() {

            return new HashSet<>(Collections.singletonList("MET")); // , "GMS", "GMN"));
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
