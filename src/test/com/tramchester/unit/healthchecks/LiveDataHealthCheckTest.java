package com.tramchester.unit.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import com.tramchester.healthchecks.LiveDataHealthCheck;
import com.tramchester.repository.LiveDataRepository;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LiveDataHealthCheckTest extends EasyMockSupport {

    LiveDataRepository repository;
    LiveDataHealthCheck healthCheck;

    @Before
    public void beforeEachTestRuns() {
        repository = createMock(LiveDataRepository.class);
        healthCheck = new LiveDataHealthCheck(repository);
    }

    @Test
    public void shouldReportUnhealthyIfNoData() {
        EasyMock.expect(repository.count()).andReturn(0);

        replayAll();
        HealthCheck.Result result = healthCheck.check();
        verifyAll();

        assertFalse(result.isHealthy());
        assertEquals("no entries present", result.getMessage());
    }

    @Test
    public void shouldReportHealthyIfHaveDataAndNoStaleEntry() {
        EasyMock.expect(repository.count()).andReturn(40);
        EasyMock.expect(repository.staleDataCount()).andReturn(0L);

        replayAll();
        HealthCheck.Result result = healthCheck.check();
        verifyAll();

        assertTrue(result.isHealthy());
    }

    @Test
    public void shouldReportUnhealthIfStaleData() {
        EasyMock.expect(repository.count()).andReturn(40);
        EasyMock.expect(repository.staleDataCount()).andReturn(2L);

        replayAll();
        HealthCheck.Result result = healthCheck.check();
        verifyAll();

        assertFalse(result.isHealthy());
        assertEquals("2 of 40 entries are stale", result.getMessage());
    }
}
