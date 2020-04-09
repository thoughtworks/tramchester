package com.tramchester.unit.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.time.TramTime;
import com.tramchester.healthchecks.LiveDataMessagesHealthCheck;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.repository.LiveDataRepository;
import com.tramchester.testSupport.TestConfig;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Path;

import static org.junit.Assert.*;

public class LiveDataMessagesHealthCheckTest extends EasyMockSupport {

    private LiveDataRepository repository;
    private LiveDataMessagesHealthCheck healthCheck;
    private ProvidesNow providesNow;

    @Before
    public void beforeEachTestRuns() {
        repository = createMock(LiveDataRepository.class);
        providesNow = createMock(ProvidesNow.class);
        TramchesterConfig config = new IntegrationTramTestConfig();
        healthCheck = new LiveDataMessagesHealthCheck(config, repository, providesNow);
    }

    @Test
    public void shouldReportUnhealthyIfNoData() {
        EasyMock.expect(repository.upToDateEntries()).andReturn(40);
        EasyMock.expect(repository.entriesWithMessages()).andReturn(0);
        EasyMock.expect(providesNow.getNow()).andReturn(TramTime.of(11,0));

        replayAll();
        HealthCheck.Result result = healthCheck.check();
        verifyAll();

        assertFalse(result.isHealthy());
        assertEquals("Not enough messages present, 0 out of 40 entries", result.getMessage());
    }

    @Test
    public void shouldNOTReportUnhealthyIfNoDataLateAtNight() {
        EasyMock.expect(repository.upToDateEntries()).andReturn(40);
        EasyMock.expect(repository.entriesWithMessages()).andReturn(0);
        EasyMock.expect(providesNow.getNow()).andReturn(TramTime.of(4,0));

        replayAll();
        HealthCheck.Result result = healthCheck.check();
        verifyAll();

        assertTrue(result.isHealthy());
    }

    @Test
    public void shouldReportUnhealthyIfHaveDataAndOverThreshold() {
        EasyMock.expect(repository.upToDateEntries()).andReturn(40);
        EasyMock.expect(repository.entriesWithMessages()).andReturn(34);
        EasyMock.expect(providesNow.getNow()).andReturn(TramTime.of(11,0));

        replayAll();
        HealthCheck.Result result = healthCheck.check();
        verifyAll();

        assertFalse(result.isHealthy());
    }

    @Test
    public void shouldNOTReportUnhealthyIfHaveDataAndOverThresholdLateAtNight() {
        EasyMock.expect(repository.upToDateEntries()).andReturn(40);
        EasyMock.expect(repository.entriesWithMessages()).andReturn(37);
        EasyMock.expect(providesNow.getNow()).andReturn(TramTime.of(3,0));

        replayAll();
        HealthCheck.Result result = healthCheck.check();
        verifyAll();

        assertTrue(result.isHealthy());
    }

    @Test
    public void shouldReportHealthyIfHaveDataAndWithinThreshold() {
        EasyMock.expect(repository.upToDateEntries()).andReturn(40);
        EasyMock.expect(repository.entriesWithMessages()).andReturn(38);
        EasyMock.expect(providesNow.getNow()).andReturn(TramTime.of(11,0));

        replayAll();
        HealthCheck.Result result = healthCheck.check();
        verifyAll();

        assertTrue(result.isHealthy());
    }

    @Test
    public void shouldReportHealthyIfHaveDataAndNoStaleEntry() {
        EasyMock.expect(repository.upToDateEntries()).andReturn(40);
        EasyMock.expect(repository.entriesWithMessages()).andReturn(40);
        EasyMock.expect(providesNow.getNow()).andReturn(TramTime.of(11,0));

        replayAll();
        HealthCheck.Result result = healthCheck.check();
        verifyAll();

        assertTrue(result.isHealthy());
    }

}
