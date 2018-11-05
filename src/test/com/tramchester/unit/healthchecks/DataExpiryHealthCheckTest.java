package com.tramchester.unit.healthchecks;


import com.codahale.metrics.health.HealthCheck;
import com.tramchester.TestConfig;
import com.tramchester.domain.FeedInfo;
import com.tramchester.healthchecks.DataExpiryHealthCheck;
import com.tramchester.repository.ProvidesFeedInfo;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Path;
import java.time.LocalDate;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

public class DataExpiryHealthCheckTest extends EasyMockSupport {

    private ProvidesFeedInfo providesFeedInfo;
    private DataExpiryHealthCheck healthCheck;

    @Before
    public void beforeEachTestRuns() {
        providesFeedInfo = createMock(ProvidesFeedInfo.class);
        FeedInfo feedInfo = createFeedInfo(LocalDate.now().minusDays(30), LocalDate.now().plusDays(3));
        EasyMock.expect(providesFeedInfo.getFeedInfo()).andReturn(feedInfo);

        healthCheck = new DataExpiryHealthCheck(providesFeedInfo, new TestConfig() {
            @Override
            public Path getDataFolder() {
                return null;
            }
        });
    }

    @Test
    public void shouldTriggerIfWithinThreshholdButNotExpiredYetMatchingNumberOfDays() {
        replayAll();
        HealthCheck.Result result = healthCheck.check();
        verifyAll();

        assertFalse(result.isHealthy());
    }

    @Test
    public void shouldTriggerIfWithinThreshholdButNotExpiredYet() {
        replayAll();
        HealthCheck.Result result = healthCheck.checkForDate(LocalDate.now().plusDays(1));
        verifyAll();

        assertFalse(result.isHealthy());
    }

    @Test
    public void shouldNotTriggerIfNotWithinThreshhold() {
        replayAll();
        HealthCheck.Result result = healthCheck.checkForDate(LocalDate.now().minusDays(1));
        verifyAll();

        assertTrue(result.isHealthy());
    }

    @Test
    public void shouldTriggerIfPastThreshhold() {
        replayAll();
        HealthCheck.Result result = healthCheck.checkForDate(LocalDate.now().plusDays(4));
        verifyAll();

        assertFalse(result.isHealthy());
    }

    private FeedInfo createFeedInfo(LocalDate validFrom, LocalDate validUntil) {
        return new FeedInfo("pubName", "pubUrl", "tz", "lang", validFrom, validUntil, "version");
    }
}
