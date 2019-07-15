package com.tramchester.unit.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import com.tramchester.domain.TramTime;
import com.tramchester.healthchecks.LiveDataHealthCheck;
import com.tramchester.healthchecks.LiveDataMessagesHealthCheck;
import com.tramchester.healthchecks.ProvidesNow;
import com.tramchester.repository.LiveDataRepository;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.Assert.*;

public class LiveDataMessagesHealthCheckTest extends EasyMockSupport {

    private LiveDataRepository repository;
    private LiveDataMessagesHealthCheck healthCheck;
    private TramTime now;

    @Before
    public void beforeEachTestRuns() {
        now = TramTime.of(LocalTime.now());
        repository = createMock(LiveDataRepository.class);
        healthCheck = new LiveDataMessagesHealthCheck(repository);
    }

    @Test
    public void shouldReportUnhealthyIfNoData() {
        EasyMock.expect(repository.countMessages()).andReturn(0);

        replayAll();
        HealthCheck.Result result = healthCheck.check();
        verifyAll();

        assertFalse(result.isHealthy());
        assertEquals("no messages present", result.getMessage());
    }

    // TODO use same count as live data ??
    @Test
    public void shouldReportHealthyIfHaveDataAndNoStaleEntry() {
        EasyMock.expect(repository.countMessages()).andReturn(40);

        replayAll();
        HealthCheck.Result result = healthCheck.check();
        verifyAll();

        assertTrue(result.isHealthy());
    }

}
