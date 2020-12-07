package com.tramchester.unit.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.healthchecks.LiveDataMessagesHealthCheck;
import com.tramchester.integration.testSupport.IntegrationTramTestConfig;
import com.tramchester.repository.PlatformMessageRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class LiveDataMessagesHealthCheckTest extends EasyMockSupport {

    private PlatformMessageRepository repository;
    private LiveDataMessagesHealthCheck healthCheck;
    private ProvidesNow providesNow;
    private StationRepository stationRepository;

    @BeforeEach
    void beforeEachTestRuns() {
        repository = createMock(PlatformMessageRepository.class);
        stationRepository = createMock(StationRepository.class);
        providesNow = createMock(ProvidesNow.class);
        TramchesterConfig config = new IntegrationTramTestConfig();
        healthCheck = new LiveDataMessagesHealthCheck(config, repository, providesNow, stationRepository);
    }

    @Test
    void shouldReportHealthy() {
        LocalDateTime daytime = LocalDateTime.of(TestEnv.testDay(), LocalTime.of(11,0));

        EasyMock.expect(providesNow.getDateTime()).andReturn(daytime);
        EasyMock.expect(repository.numberOfEntries()).andReturn(42);
        EasyMock.expect(stationRepository.getNumberOfStations()).andReturn(123);
        EasyMock.expect(repository.numberStationsWithMessages(daytime)).andReturn(123);

        replayAll();
        HealthCheck.Result result = healthCheck.check();
        verifyAll();

        assertTrue(result.isHealthy());
    }

    @Test
    void shouldReportHealthyIfEnoughStationsWithinThreshhold() {
        LocalDateTime daytime = LocalDateTime.of(TestEnv.testDay(), LocalTime.of(11,0));

        EasyMock.expect(providesNow.getDateTime()).andReturn(daytime);
        EasyMock.expect(repository.numberOfEntries()).andReturn(42);

        EasyMock.expect(stationRepository.getNumberOfStations()).andReturn(123);
        EasyMock.expect(repository.numberStationsWithMessages(daytime)).andReturn(119);

        replayAll();
        HealthCheck.Result result = healthCheck.check();
        verifyAll();

        assertTrue(result.isHealthy());
    }

    @Test
    void shouldReportUnhealthyIfNoData() {
        EasyMock.expect(repository.numberOfEntries()).andReturn(0);

        replayAll();
        HealthCheck.Result result = healthCheck.check();
        verifyAll();

        assertFalse(result.isHealthy());
        assertEquals("No entires present", result.getMessage());
    }

    @Test
    void shouldReportUnhealthyIfMissingStations() {
        LocalDateTime daytime = LocalDateTime.of(TestEnv.testDay(), LocalTime.of(11,0));

        EasyMock.expect(providesNow.getDateTime()).andReturn(daytime);
        EasyMock.expect(repository.numberOfEntries()).andReturn(42);
        EasyMock.expect(stationRepository.getNumberOfStations()).andReturn(123);
        EasyMock.expect(repository.numberStationsWithMessages(daytime)).andReturn(10);

        replayAll();
        HealthCheck.Result result = healthCheck.check();
        verifyAll();

        assertFalse(result.isHealthy());
        assertEquals("Not enough messages present, 42 entries, 10 out of 123 stations", result.getMessage());
    }

    @Test
    void shouldNOTReportUnhealthyIfNoDataLateAtNight() {
        LocalDateTime lateNight = LocalDateTime.of(TestEnv.testDay(), LocalTime.of(4,0));

        EasyMock.expect(providesNow.getDateTime()).andReturn(lateNight);
        EasyMock.expect(repository.numberOfEntries()).andReturn(42);
        EasyMock.expect(stationRepository.getNumberOfStations()).andReturn(123);
        EasyMock.expect(repository.numberStationsWithMessages(lateNight)).andReturn(10);

        replayAll();
        HealthCheck.Result result = healthCheck.check();
        verifyAll();

        assertTrue(result.isHealthy());
    }


}
