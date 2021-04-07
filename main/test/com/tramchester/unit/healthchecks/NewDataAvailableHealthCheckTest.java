package com.tramchester.unit.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.dataimport.FetchFileModTime;
import com.tramchester.dataimport.URLDownloadAndModTime;
import com.tramchester.domain.ServiceTimeLimits;
import com.tramchester.healthchecks.NewDataAvailableHealthCheck;
import com.tramchester.testSupport.TestEnv;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDateTime;

class NewDataAvailableHealthCheckTest extends EasyMockSupport {

    private URLDownloadAndModTime urlDownloader;
    private FetchFileModTime fetchFileModTime;
    private NewDataAvailableHealthCheck healthCheck;
    private String expectedURL;
    private LocalDateTime time;
    private final RemoteDataSourceConfig config = TestEnv.GET().getRemoteDataSourceConfig().get(0);

    @BeforeEach
    void beforeEachTestRuns() {
        urlDownloader = createMock(URLDownloadAndModTime.class);
        fetchFileModTime = createMock(FetchFileModTime.class);
        expectedURL = config.getDataUrl();
        ServiceTimeLimits serviceTimeLimits = new ServiceTimeLimits();

        healthCheck = new NewDataAvailableHealthCheck(config, urlDownloader, fetchFileModTime, serviceTimeLimits);
        time = TestEnv.LocalNow();
    }

    @Test
    void shouldReportHealthyWhenNONewDataAvailable() throws IOException {

        EasyMock.expect(urlDownloader.getModTime(expectedURL)).andReturn(time.minusDays(1));
        EasyMock.expect(fetchFileModTime.getFor(config)).andReturn(time);

        replayAll();
        HealthCheck.Result result = healthCheck.execute();
        Assertions.assertTrue(result.isHealthy());
        verifyAll();
    }

    @Test
    void shouldReportUnHealthyWhenNewDataAvailable() throws IOException {

        EasyMock.expect(urlDownloader.getModTime(expectedURL)).andReturn(time.plusDays(1));
        EasyMock.expect(fetchFileModTime.getFor(config)).andReturn(time);

        replayAll();
        HealthCheck.Result result = healthCheck.execute();
        Assertions.assertFalse(result.isHealthy());
        verifyAll();
    }

    @Test
    void shouldReportUnHealthyWhenDataMissing() throws IOException {

        EasyMock.expect(urlDownloader.getModTime(expectedURL)).andReturn(LocalDateTime.MIN);
        EasyMock.expect(fetchFileModTime.getFor(config)).andReturn(time);

        replayAll();
        HealthCheck.Result result = healthCheck.execute();
        Assertions.assertFalse(result.isHealthy());
        verifyAll();
    }

}
