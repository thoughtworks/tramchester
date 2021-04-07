package com.tramchester.unit.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.FetchFileModTime;
import com.tramchester.dataimport.URLDownloadAndModTime;
import com.tramchester.domain.ServiceTimeLimits;
import com.tramchester.healthchecks.NewDataAvailableHealthCheck;
import com.tramchester.integration.testSupport.TFGMRemoteDataSourceConfig;
import com.tramchester.testSupport.TestConfig;
import com.tramchester.testSupport.TestEnv;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

class NewDataAvailableHealthCheckTest extends EasyMockSupport {

    private URLDownloadAndModTime urlDownloader;
    private FetchFileModTime fetchFileModTime;
    private NewDataAvailableHealthCheck healthCheck;
    private String expectedURL;
    private LocalDateTime time;
    private RemoteDataSourceConfig dataSourceConfig;

    @BeforeEach
    void beforeEachTestRuns() throws IOException {
        TramchesterConfig config = new LocalTestConfig(Files.createTempDirectory("FetchDataFromUrlTest"));
        urlDownloader = createMock(URLDownloadAndModTime.class);
        fetchFileModTime = createMock(FetchFileModTime.class);
        dataSourceConfig = config.getRemoteDataSourceConfig().get(0);
        expectedURL = dataSourceConfig.getDataUrl();
        ServiceTimeLimits serviceTimeLimits = new ServiceTimeLimits();

        healthCheck = new NewDataAvailableHealthCheck(dataSourceConfig, urlDownloader, fetchFileModTime, serviceTimeLimits);
        time = TestEnv.LocalNow();
    }

    @AfterEach
    void removeTmpFile() throws IOException {
        Path tmpDir = dataSourceConfig.getDataPath();
        if (Files.exists(tmpDir)) {
            Files.delete(tmpDir);
        }
    }

    @Test
    void shouldReportHealthyWhenNONewDataAvailable() throws IOException {

        EasyMock.expect(urlDownloader.getModTime(expectedURL)).andReturn(time.minusDays(1));
        EasyMock.expect(fetchFileModTime.getFor(dataSourceConfig)).andReturn(time);

        replayAll();
        HealthCheck.Result result = healthCheck.execute();
        Assertions.assertTrue(result.isHealthy());
        verifyAll();
    }

    @Test
    void shouldReportUnHealthyWhenNewDataAvailable() throws IOException {

        EasyMock.expect(urlDownloader.getModTime(expectedURL)).andReturn(time.plusDays(1));
        EasyMock.expect(fetchFileModTime.getFor(dataSourceConfig)).andReturn(time);

        replayAll();
        HealthCheck.Result result = healthCheck.execute();
        Assertions.assertFalse(result.isHealthy());
        verifyAll();
    }

    @Test
    void shouldReportUnHealthyWhenDataMissing() throws IOException {

        EasyMock.expect(urlDownloader.getModTime(expectedURL)).andReturn(LocalDateTime.MIN);
        EasyMock.expect(fetchFileModTime.getFor(dataSourceConfig)).andReturn(time);

        replayAll();
        HealthCheck.Result result = healthCheck.execute();
        Assertions.assertFalse(result.isHealthy());
        verifyAll();
    }

    private static class LocalTestConfig extends TestConfig {
        private final Path dataPath;

        private LocalTestConfig(Path dataPath) {
            this.dataPath = dataPath;
        }

        @Override
        protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
            return null;
        }

        @Override
        public List<RemoteDataSourceConfig> getRemoteDataSourceConfig() {
            return Collections.singletonList(new TFGMRemoteDataSourceConfig(dataPath));
        }
    }

}
