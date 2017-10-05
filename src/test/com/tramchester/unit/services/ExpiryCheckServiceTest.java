package com.tramchester.unit.services;


import com.tramchester.domain.FeedInfo;
import com.tramchester.repository.ProvidesFeedInfo;
import com.tramchester.services.ExpiryCheckService;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

public class ExpiryCheckServiceTest extends EasyMockSupport {

    private ProvidesFeedInfo providesFeedInfo;
    private Optional<Boolean> reported = Optional.empty();
    private ExpiryCheckService service;

    @Before
    public void beforeEachTestRuns() {
        providesFeedInfo = createMock(ProvidesFeedInfo.class);
        FeedInfo feedInfo = createFeedInfo(LocalDate.now().minusDays(30), LocalDate.now().plusDays(3));
        EasyMock.expect(providesFeedInfo.getFeedInfo()).andReturn(feedInfo);

        int threshhold = 3; //days

        service = new ExpiryCheckService(providesFeedInfo, threshhold);
    }

    @Test
    public void shouldTriggerIfWithinThreshholdButNotExpiredYetMatchingNumberOfDays() {
        replayAll();
        service.check(LocalDate.now(), (hasAlreadyExpired, validUntil) -> reported = Optional.of(hasAlreadyExpired));
        verifyAll();

        assertTrue(reported.isPresent());
        assertFalse(reported.get());
    }

    @Test
    public void shouldTriggerIfWithinThreshholdButNotExpiredYet() {
        replayAll();
        service.check(LocalDate.now().plusDays(1), (hasAlreadyExpired, validUntil) -> reported = Optional.of(hasAlreadyExpired));
        verifyAll();

        assertTrue(reported.isPresent());
        assertFalse(reported.get());
    }

    @Test
    public void shouldNotTriggerIfNotWithinThreshhold() {
        replayAll();
        service.check(LocalDate.now().minusDays(1), (hasAlreadyExpired, validUntil) -> reported = Optional.of(hasAlreadyExpired));
        verifyAll();

        assertFalse(reported.isPresent());
    }

    @Test
    public void shouldTriggerIfPastThreshhold() {
        replayAll();
        service.check(LocalDate.now().plusDays(4), (hasAlreadyExpired, validUntil) -> reported = Optional.of(hasAlreadyExpired));
        verifyAll();

        assertTrue(reported.isPresent());
        assertTrue(reported.get());
    }

    private FeedInfo createFeedInfo(LocalDate validFrom, LocalDate validUntil) {
        return new FeedInfo("pubName", "pubUrl", "tz", "lang", validFrom, validUntil, "version");
    }
}
