package com.tramchester.unit.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.ServiceTimeLimits;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.healthchecks.LiveDataS3UploadHealthCheck;
import com.tramchester.livedata.CountsUploadedLiveData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TestLiveDataConfig;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LiveDataS3UploadHealthCheckTest extends EasyMockSupport {

    private final TramchesterConfig configuration = TestEnv.GET(new TestLiveDataConfig());
    private LocalDateTime localNow;
    private ProvidesLocalNow providesLocalNow;
    private CountsUploadedLiveData countsUploadedLiveData;
    private LiveDataS3UploadHealthCheck healthCheck;
    private Duration expectedDuration;

    @BeforeEach
    void beforeEachTest() {
        localNow = TestEnv.LocalNow();
        providesLocalNow = createMock(ProvidesLocalNow.class);
        countsUploadedLiveData = createMock(CountsUploadedLiveData.class);
        ServiceTimeLimits serviceTimeLimits = new ServiceTimeLimits();

        healthCheck = new LiveDataS3UploadHealthCheck(providesLocalNow,
                countsUploadedLiveData, configuration, serviceTimeLimits);
        expectedDuration = Duration.of(2 * configuration.getLiveDataConfig().getRefreshPeriodSeconds(), ChronoUnit.SECONDS);
    }

    @Test
    void shouldReportHealthIfUpToDateDataIsInS3() throws Exception {
        EasyMock.expect(providesLocalNow.getDateTime()).andStubReturn(localNow);
        EasyMock.expect(countsUploadedLiveData.count(localNow.minus(expectedDuration), expectedDuration))
                .andReturn(1L);

        replayAll();
        healthCheck.start();
        HealthCheck.Result result = healthCheck.check();
        verifyAll();

        assertTrue(result.isHealthy());

    }

    @Test
    void shouldReportUnhealthIfNoDataFound() throws Exception {
        EasyMock.expect(providesLocalNow.getDateTime()).andStubReturn(localNow);
        EasyMock.expect(countsUploadedLiveData.count(localNow.minus(expectedDuration), expectedDuration))
                .andReturn(0L);

        replayAll();
        healthCheck.start();
        HealthCheck.Result result = healthCheck.check();
        verifyAll();

        assertFalse(result.isHealthy());
    }
}
