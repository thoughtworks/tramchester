package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.config.DataSourceConfig;
import com.tramchester.domain.GTFSTransportationType;
import com.tramchester.integration.TFGMTestDataSourceConfig;
import com.tramchester.testSupport.TestConfig;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

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

//        @Override
//        public Path getDataFolder() {
//            return Paths.get("data/all");
//        }

        @Override
        protected List<DataSourceConfig> getDataSourceFORTESTING() {
            HashSet<GTFSTransportationType> modes = TestEnv.tramAndBus;
            return Collections.singletonList(new TFGMTestDataSourceConfig("data/busAndTram", modes));
        }
    }
}
