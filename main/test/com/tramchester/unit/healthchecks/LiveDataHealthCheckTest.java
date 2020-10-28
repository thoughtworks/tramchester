package com.tramchester.unit.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.healthchecks.LiveDataHealthCheck;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.repository.DueTramsRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;

class LiveDataHealthCheckTest extends EasyMockSupport {

    private DueTramsRepository repository;
    private LiveDataHealthCheck healthCheck;
    private StationRepository stationRepository;
    private ProvidesNow providesNow;

    @BeforeEach
    void beforeEachTestRuns() {
        repository = createMock(DueTramsRepository.class);
        stationRepository = createMock(StationRepository.class);
        providesNow = createMock(ProvidesNow.class);
        TramchesterConfig config =  new IntegrationTramTestConfig();

        healthCheck = new LiveDataHealthCheck(repository, providesNow, stationRepository, config);
    }

    @Test
    void shouldReportUnhealthyIfNoData() {
        EasyMock.expect(repository.upToDateEntries()).andReturn(0);

        replayAll();
        HealthCheck.Result result = healthCheck.check();
        verifyAll();

        Assertions.assertFalse(result.isHealthy());
        Assertions.assertEquals("no entries present", result.getMessage());
    }

    @Test
    void shouldReportHealthyWithAllStations() {
        LocalDateTime now = TestEnv.LocalNow();
        EasyMock.expect(providesNow.getDateTime()).andReturn(now);

        EasyMock.expect(repository.upToDateEntries()).andReturn(40);
        EasyMock.expect(stationRepository.getNumberOfStations()).andReturn(87);
        EasyMock.expect(repository.getNumStationsWithData(now)).andReturn(87);

        replayAll();
        HealthCheck.Result result = healthCheck.check();
        verifyAll();

        Assertions.assertTrue(result.isHealthy());
    }

    @Test
    void shouldReportHealthyWithAllStationsWithinThreshhold() {
        LocalDateTime now = TestEnv.LocalNow();
        EasyMock.expect(providesNow.getDateTime()).andReturn(now);

        EasyMock.expect(repository.upToDateEntries()).andReturn(40);
        EasyMock.expect(stationRepository.getNumberOfStations()).andReturn(87);
        EasyMock.expect(repository.getNumStationsWithData(now)).andReturn(83);

        replayAll();
        HealthCheck.Result result = healthCheck.check();
        verifyAll();

        Assertions.assertTrue(result.isHealthy());
    }

    @Test
    void shouldReportUnhealthyIfGoodEntriesButNoEnoughStations() {
        LocalDateTime now = TestEnv.LocalNow();
        EasyMock.expect(providesNow.getDateTime()).andReturn(now);

        EasyMock.expect(repository.upToDateEntries()).andReturn(40);
        EasyMock.expect(stationRepository.getNumberOfStations()).andReturn(111);
        EasyMock.expect(repository.getNumStationsWithData(now)).andReturn(3);

        replayAll();
        HealthCheck.Result result = healthCheck.check();
        verifyAll();

        Assertions.assertFalse(result.isHealthy());
        Assertions.assertEquals("Only 3 of 111 stations have data, 40 entries present", result.getMessage());
    }

    @Test
    void shouldNOTReportUnhealthyIfGoodEntriesButNoEnoughStationsLateNight() {
        LocalDateTime now = LocalDateTime.of(TestEnv.testDay(), LocalTime.of(4,0));
        EasyMock.expect(providesNow.getDateTime()).andReturn(now);

        EasyMock.expect(repository.upToDateEntries()).andReturn(40);
        EasyMock.expect(stationRepository.getNumberOfStations()).andReturn(111);
        EasyMock.expect(repository.getNumStationsWithData(now)).andReturn(40);

        replayAll();
        HealthCheck.Result result = healthCheck.check();
        verifyAll();

        Assertions.assertTrue(result.isHealthy());
    }

}
