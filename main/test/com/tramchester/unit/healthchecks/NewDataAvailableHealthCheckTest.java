package com.tramchester.unit.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.FetchDataFromUrl;
import com.tramchester.dataimport.FileModTime;
import com.tramchester.dataimport.URLDownloader;
import com.tramchester.healthchecks.NewDataAvailableHealthCheck;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.testSupport.TestEnv;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;

class NewDataAvailableHealthCheckTest extends EasyMockSupport {

    private URLDownloader urlDownloader;
    private final TramchesterConfig config = new IntegrationTramTestConfig();
    private FileModTime fileModTime;
    private Path expectedPath;
    private NewDataAvailableHealthCheck healthCheck;
    private String expectedURL;
    private LocalDateTime time;

    @BeforeEach
    void beforeEachTestRuns() {
        urlDownloader = createMock(URLDownloader.class);
        fileModTime = createMock(FileModTime.class);
        expectedPath = config.getDataPath().resolve(FetchDataFromUrl.ZIP_FILENAME);
        expectedURL = config.getTramDataCheckUrl();

        healthCheck = new NewDataAvailableHealthCheck(config, urlDownloader, fileModTime);
        time = TestEnv.LocalNow();
    }

    @Test
    void shouldReportHealthyWhenNONewDataAvailable() throws IOException {

        EasyMock.expect(urlDownloader.getModTime(expectedURL)).andReturn(time.minusDays(1));
        EasyMock.expect(fileModTime.getFor(expectedPath)).andReturn(time);

        replayAll();
        HealthCheck.Result result = healthCheck.execute();
        Assertions.assertTrue(result.isHealthy());
        verifyAll();
    }

    @Test
    void shouldReportUnHealthyWhenNewDataAvailable() throws IOException {

        EasyMock.expect(urlDownloader.getModTime(expectedURL)).andReturn(time.plusDays(1));
        EasyMock.expect(fileModTime.getFor(expectedPath)).andReturn(time);

        replayAll();
        HealthCheck.Result result = healthCheck.execute();
        Assertions.assertFalse(result.isHealthy());
        verifyAll();
    }

}
