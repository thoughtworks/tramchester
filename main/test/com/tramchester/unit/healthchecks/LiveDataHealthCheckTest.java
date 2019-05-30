package com.tramchester.unit.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import com.tramchester.domain.TramTime;
import com.tramchester.healthchecks.LiveDataHealthCheck;
import com.tramchester.healthchecks.ProvidesNow;
import com.tramchester.repository.LiveDataRepository;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LiveDataHealthCheckTest extends EasyMockSupport {

    private LiveDataRepository repository;
    private LiveDataHealthCheck healthCheck;
    private TramTime now;

    @Before
    public void beforeEachTestRuns() {
        now = TramTime.of(LocalTime.now());
        repository = createMock(LiveDataRepository.class);
        healthCheck = new LiveDataHealthCheck(repository, () -> now);
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
        EasyMock.expect(repository.upToDateEntries(now)).andReturn(40L);

        replayAll();
        HealthCheck.Result result = healthCheck.check();
        verifyAll();

        assertTrue(result.isHealthy());
    }

    @Test
    public void shouldReportUnhealthIfStaleDate() {
        EasyMock.expect(repository.count()).andReturn(40);
        EasyMock.expect(repository.staleDataCount()).andReturn(2L);

        replayAll();
        HealthCheck.Result result = healthCheck.check();
        verifyAll();

        assertFalse(result.isHealthy());
        assertEquals("2 of 40 entries are stale", result.getMessage());
    }

    @Test
    public void shouldReportUnhealthIfStaleTime() {
        EasyMock.expect(repository.count()).andReturn(40);
        EasyMock.expect(repository.upToDateEntries(now)).andReturn(20L);
        EasyMock.expect(repository.staleDataCount()).andReturn(0L);

        replayAll();
        HealthCheck.Result result = healthCheck.check();
        verifyAll();

        assertFalse(result.isHealthy());
        assertEquals("20 of 40 entries are expired at "+now.toString(), result.getMessage());
    }
}
