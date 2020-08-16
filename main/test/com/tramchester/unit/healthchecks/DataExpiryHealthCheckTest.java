package com.tramchester.unit.healthchecks;


import com.codahale.metrics.health.HealthCheck;
import com.tramchester.domain.FeedInfo;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.healthchecks.DataExpiryHealthCheck;
import com.tramchester.testSupport.TestEnv;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

class DataExpiryHealthCheckTest extends EasyMockSupport {

    private DataExpiryHealthCheck healthCheck;
    private LocalDate localDate;

    @BeforeEach
    void beforeEachTestRuns() {
        ProvidesLocalNow providesLocalNow = createMock(ProvidesLocalNow.class);

        localDate = TestEnv.LocalNow().toLocalDate();
        FeedInfo feedInfo = createFeedInfo(localDate.minusDays(30), localDate.plusDays(3));
        EasyMock.expect(providesLocalNow.getDate()).andStubReturn(localDate);

        healthCheck = new DataExpiryHealthCheck(feedInfo, "feedName", providesLocalNow, TestEnv.GET());
    }

    @Test
    void shouldTriggerIfWithinThreshholdButNotExpiredYetMatchingNumberOfDays() {
        replayAll();
        HealthCheck.Result result = healthCheck.check();
        verifyAll();

        Assertions.assertFalse(result.isHealthy());
    }

    @Test
    void shouldTriggerIfWithinThreshholdButNotExpiredYet() {
        replayAll();
        HealthCheck.Result result = healthCheck.checkForDate(localDate.plusDays(1));
        verifyAll();

        Assertions.assertFalse(result.isHealthy());
    }

    @Test
    void shouldNotTriggerIfNotWithinThreshhold() {
        replayAll();
        HealthCheck.Result result = healthCheck.checkForDate(localDate.minusDays(1));
        verifyAll();

        Assertions.assertTrue(result.isHealthy());
    }

    @Test
    void shouldTriggerIfPastThreshhold() {
        replayAll();
        HealthCheck.Result result = healthCheck.checkForDate(localDate.plusDays(4));
        verifyAll();

        Assertions.assertFalse(result.isHealthy());
    }

    private FeedInfo createFeedInfo(LocalDate validFrom, LocalDate validUntil) {
        return new FeedInfo("pubName", "pubUrl", "tz", "lang", validFrom, validUntil, "version");
    }
}
