package com.tramchester.unit.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.healthchecks.LiveDataMessagesHealthCheck;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.repository.LiveDataUpdater;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LiveDataMessagesHealthCheckTest extends EasyMockSupport {

    private LiveDataUpdater repository;
    private LiveDataMessagesHealthCheck healthCheck;
    private ProvidesNow providesNow;

    @BeforeEach
    void beforeEachTestRuns() {
        repository = createMock(LiveDataUpdater.class);
        providesNow = createMock(ProvidesNow.class);
        TramchesterConfig config = new IntegrationTramTestConfig();
        healthCheck = new LiveDataMessagesHealthCheck(config, repository, providesNow);
    }

    @Test
    void shouldReportUnhealthyIfNoData() {
        EasyMock.expect(repository.upToDateEntries()).andReturn(40);
        EasyMock.expect(repository.countEntriesWithMessages()).andReturn(0);
        EasyMock.expect(providesNow.getNow()).andReturn(TramTime.of(11,0));

        replayAll();
        HealthCheck.Result result = healthCheck.check();
        verifyAll();

        assertFalse(result.isHealthy());
        assertEquals("Not enough messages present, 0 out of 40 entries", result.getMessage());
    }

    @Test
    void shouldNOTReportUnhealthyIfNoDataLateAtNight() {
        EasyMock.expect(repository.upToDateEntries()).andReturn(40);
        EasyMock.expect(repository.countEntriesWithMessages()).andReturn(0);
        EasyMock.expect(providesNow.getNow()).andReturn(TramTime.of(4,0));

        replayAll();
        HealthCheck.Result result = healthCheck.check();
        verifyAll();

        assertTrue(result.isHealthy());
    }

    @Test
    void shouldReportUnhealthyIfHaveDataAndOverThreshold() {
        EasyMock.expect(repository.upToDateEntries()).andReturn(40);
        EasyMock.expect(repository.countEntriesWithMessages()).andReturn(34);
        EasyMock.expect(providesNow.getNow()).andReturn(TramTime.of(11,0));

        replayAll();
        HealthCheck.Result result = healthCheck.check();
        verifyAll();

        assertFalse(result.isHealthy());
    }

    @Test
    void shouldNOTReportUnhealthyIfHaveDataAndOverThresholdLateAtNight() {
        EasyMock.expect(repository.upToDateEntries()).andReturn(40);
        EasyMock.expect(repository.countEntriesWithMessages()).andReturn(37);
        EasyMock.expect(providesNow.getNow()).andReturn(TramTime.of(3,0));

        replayAll();
        HealthCheck.Result result = healthCheck.check();
        verifyAll();

        assertTrue(result.isHealthy());
    }

    @Test
    void shouldReportHealthyIfHaveDataAndWithinThreshold() {
        EasyMock.expect(repository.upToDateEntries()).andReturn(40);
        EasyMock.expect(repository.countEntriesWithMessages()).andReturn(38);
        EasyMock.expect(providesNow.getNow()).andReturn(TramTime.of(11,0));

        replayAll();
        HealthCheck.Result result = healthCheck.check();
        verifyAll();

        assertTrue(result.isHealthy());
    }

    @Test
    void shouldReportHealthyIfHaveDataAndNoStaleEntry() {
        EasyMock.expect(repository.upToDateEntries()).andReturn(40);
        EasyMock.expect(repository.countEntriesWithMessages()).andReturn(40);
        EasyMock.expect(providesNow.getNow()).andReturn(TramTime.of(11,0));

        replayAll();
        HealthCheck.Result result = healthCheck.check();
        verifyAll();

        assertTrue(result.isHealthy());
    }

}
