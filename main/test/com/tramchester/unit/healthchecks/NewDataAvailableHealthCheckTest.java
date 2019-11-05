package com.tramchester.unit.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.FetchDataFromUrl;
import com.tramchester.dataimport.FileModTime;
import com.tramchester.dataimport.URLDownloader;
import com.tramchester.healthchecks.NewDataAvailableHealthCheck;
import com.tramchester.integration.IntegrationTramTestConfig;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertTrue;

public class NewDataAvailableHealthCheckTest extends EasyMockSupport {

    private URLDownloader urlDownloader;
    private TramchesterConfig config = new IntegrationTramTestConfig();
    private FileModTime fileModTime;
    private Path expectedPath;
    private NewDataAvailableHealthCheck healthCheck;
    private String expectedURL;

    @Before
    public void beforeEachTestRuns() {
        urlDownloader = createMock(URLDownloader.class);
        fileModTime = createMock(FileModTime.class);
        expectedPath = config.getDataPath().resolve(FetchDataFromUrl.ZIP_FILENAME);
        expectedURL = config.getTramDataCheckUrl();

        healthCheck = new NewDataAvailableHealthCheck(config, urlDownloader, fileModTime);
    }

    @Test
    public void shouldReportHealthyWhenNONewDataAvailable() throws IOException {
        LocalDateTime time = LocalDateTime.now();

        EasyMock.expect(urlDownloader.getModTime(expectedURL)).andReturn(time.minusDays(1));
        EasyMock.expect(fileModTime.getFor(expectedPath)).andReturn(time);

        replayAll();
        HealthCheck.Result result = healthCheck.execute();
        assertTrue(result.isHealthy());
        verifyAll();
    }

    @Test
    public void shouldReportUnHealthyWhenNewDataAvailable() throws IOException {
        LocalDateTime time = LocalDateTime.now();

        EasyMock.expect(urlDownloader.getModTime(expectedURL)).andReturn(time.plusDays(1));
        EasyMock.expect(fileModTime.getFor(expectedPath)).andReturn(time);

        replayAll();
        HealthCheck.Result result = healthCheck.execute();
        assertFalse(result.isHealthy());
        verifyAll();
    }

}
