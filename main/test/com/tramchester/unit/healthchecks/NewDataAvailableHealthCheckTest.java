package com.tramchester.unit.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import com.tramchester.domain.FeedInfo;
import com.tramchester.healthchecks.NewDataAvailableHealthCheck;
import com.tramchester.repository.LatestFeedInfoRepository;
import com.tramchester.repository.ProvidesFeedInfo;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertTrue;

public class NewDataAvailableHealthCheckTest extends EasyMockSupport {

    private LatestFeedInfoRepository repository;
    private ProvidesFeedInfo current;

    @Before
    public void beforeEachTestRuns() {
        repository = createMock(LatestFeedInfoRepository.class);
        current = createMock(ProvidesFeedInfo.class);
    }

    @Test
    public void shouldReportHealthyWhenNewDataAvailable() {
        NewDataAvailableHealthCheck healthCheck = new NewDataAvailableHealthCheck(repository, current);

        EasyMock.expect(repository.getFeedinfo()).andReturn(createFeedinfo(LocalDate.of(2019, 11, 15), LocalDate.of(2019, 12, 14)));
        EasyMock.expect(current.getFeedInfo()).andReturn(createFeedinfo(LocalDate.of(2019, 11, 15), LocalDate.of(2019, 12, 14)));

        replayAll();
        HealthCheck.Result result = healthCheck.execute();
        assertTrue(result.isHealthy());

        verifyAll();
    }

    @Test
    public void shouldReportUnHealthyWhenNewDataAvailable() {
        NewDataAvailableHealthCheck healthCheck = new NewDataAvailableHealthCheck(repository, current);

        EasyMock.expect(repository.getFeedinfo()).andReturn(createFeedinfo(LocalDate.of(2019, 11, 15), LocalDate.of(2019, 12, 14)));
        EasyMock.expect(current.getFeedInfo()).andReturn(createFeedinfo(LocalDate.of(2019, 11, 30), LocalDate.of(2019, 12, 24)));

        replayAll();
        HealthCheck.Result result = healthCheck.execute();
        assertFalse(result.isHealthy());

        verifyAll();
    }

    private FeedInfo createFeedinfo(LocalDate validFrom, LocalDate validUntil) {
        return new FeedInfo("publisherName", "publisherUrl", "timezone",
                "lang", validFrom,
                validUntil, "version");
    }
}
