package com.tramchester.unit.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import com.tramchester.healthchecks.LiveDataMessagesHealthCheck;
import com.tramchester.repository.LiveDataRepository;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class LiveDataMessagesHealthCheckTest extends EasyMockSupport {

    private LiveDataRepository repository;
    private LiveDataMessagesHealthCheck healthCheck;

    @Before
    public void beforeEachTestRuns() {
        repository = createMock(LiveDataRepository.class);
        healthCheck = new LiveDataMessagesHealthCheck(repository);
    }

    @Test
    public void shouldReportUnhealthyIfNoData() {
        EasyMock.expect(repository.countEntries()).andReturn(40);
        EasyMock.expect(repository.countMessages()).andReturn(0);

        replayAll();
        HealthCheck.Result result = healthCheck.check();
        verifyAll();

        assertFalse(result.isHealthy());
        assertEquals("Not enough messages present, 0 out of 40 entries", result.getMessage());
    }

    // TODO may need to add a threshhold should as 50%
    @Test
    public void shouldReportHealthyIfHaveDataAndNoStaleEntry() {
        EasyMock.expect(repository.countEntries()).andReturn(40);
        EasyMock.expect(repository.countMessages()).andReturn(40);

        replayAll();
        HealthCheck.Result result = healthCheck.check();
        verifyAll();

        assertTrue(result.isHealthy());
    }

}
