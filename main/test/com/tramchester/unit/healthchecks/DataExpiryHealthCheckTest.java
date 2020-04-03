package com.tramchester.unit.healthchecks;


import com.codahale.metrics.health.HealthCheck;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.FeedInfo;
import com.tramchester.healthchecks.DataExpiryHealthCheck;
import com.tramchester.repository.ProvidesFeedInfo;
import com.tramchester.testSupport.TestEnv;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

public class DataExpiryHealthCheckTest extends EasyMockSupport {

    private ProvidesFeedInfo providesFeedInfo;
    private DataExpiryHealthCheck healthCheck;
    private LocalDate localDate;
    private ProvidesLocalNow providesLocalNow;

    @Before
    public void beforeEachTestRuns() {
        providesFeedInfo = createMock(ProvidesFeedInfo.class);
        providesLocalNow = createMock(ProvidesLocalNow.class);

        localDate = TestEnv.LocalNow().toLocalDate();
        FeedInfo feedInfo = createFeedInfo(localDate.minusDays(30), localDate.plusDays(3));
        EasyMock.expect(providesFeedInfo.getFeedInfo()).andReturn(feedInfo);
        EasyMock.expect(providesLocalNow.getDate()).andStubReturn(localDate);

        healthCheck = new DataExpiryHealthCheck(providesFeedInfo, providesLocalNow, TestEnv.GET());
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
        HealthCheck.Result result = healthCheck.checkForDate(localDate.plusDays(1));
        verifyAll();

        assertFalse(result.isHealthy());
    }

    @Test
    public void shouldNotTriggerIfNotWithinThreshhold() {
        replayAll();
        HealthCheck.Result result = healthCheck.checkForDate(localDate.minusDays(1));
        verifyAll();

        assertTrue(result.isHealthy());
    }

    @Test
    public void shouldTriggerIfPastThreshhold() {
        replayAll();
        HealthCheck.Result result = healthCheck.checkForDate(localDate.plusDays(4));
        verifyAll();

        assertFalse(result.isHealthy());
    }

    private FeedInfo createFeedInfo(LocalDate validFrom, LocalDate validUntil) {
        return new FeedInfo("pubName", "pubUrl", "tz", "lang", validFrom, validUntil, "version");
    }
}
