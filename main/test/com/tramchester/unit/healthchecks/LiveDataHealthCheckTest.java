package com.tramchester.unit.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.ServiceTimeLimits;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.healthchecks.LiveDataHealthCheck;
import com.tramchester.livedata.tfgm.TramDepartureRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TestTramLiveDataConfig;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

class LiveDataHealthCheckTest extends EasyMockSupport {

    private TramDepartureRepository repository;
    private LiveDataHealthCheck healthCheck;
    private StationRepository stationRepository;
    private ProvidesNow providesNow;

    @BeforeEach
    void beforeEachTestRuns() {
        repository = createMock(TramDepartureRepository.class);
        stationRepository = createMock(StationRepository.class);
        providesNow = createMock(ProvidesNow.class);
        TramchesterConfig config = TestEnv.GET(new TestTramLiveDataConfig());
        ServiceTimeLimits serviceTimeLimits = new ServiceTimeLimits();

        healthCheck = new LiveDataHealthCheck(repository, providesNow, stationRepository, config, serviceTimeLimits);
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
        EasyMock.expect(stationRepository.getNumberOfStations(DataSourceID.tfgm, TransportMode.Tram)).andReturn(87L);
        EasyMock.expect(repository.getNumStationsWithData(now)).andReturn(87);
        EasyMock.expect(repository.getNumStationsWithTrams(now)).andReturn(87);

        replayAll();
        healthCheck.start();
        HealthCheck.Result result = healthCheck.check();
        verifyAll();

        Assertions.assertTrue(result.isHealthy());
    }

    @Test
    void shouldReportHealthyWithAllStationsWithinThreshhold() {
        LocalDateTime now = TestEnv.LocalNow();
        EasyMock.expect(providesNow.getDateTime()).andReturn(now);

        EasyMock.expect(repository.upToDateEntries()).andReturn(40);
        EasyMock.expect(stationRepository.getNumberOfStations(DataSourceID.tfgm, TransportMode.Tram)).andReturn(87L);
        EasyMock.expect(repository.getNumStationsWithData(now)).andReturn(83);
        EasyMock.expect(repository.getNumStationsWithTrams(now)).andReturn(83);

        replayAll();
        healthCheck.start();
        HealthCheck.Result result = healthCheck.check();
        verifyAll();

        Assertions.assertTrue(result.isHealthy());
    }

    @Test
    void shouldReportUnhealthyIfGoodEntriesButNoEnoughStations() {
        LocalDate date = TestEnv.LocalNow().toLocalDate();
        LocalDateTime now = LocalDateTime.of(date, LocalTime.of(15,42));
        EasyMock.expect(providesNow.getDateTime()).andReturn(now);

        EasyMock.expect(repository.upToDateEntries()).andReturn(40);
        EasyMock.expect(stationRepository.getNumberOfStations(DataSourceID.tfgm, TransportMode.Tram)).andReturn(111L);
        EasyMock.expect(repository.getNumStationsWithData(now)).andReturn(3);
        EasyMock.expect(repository.getNumStationsWithTrams(now)).andReturn(83);


        replayAll();
        healthCheck.start();
        HealthCheck.Result result = healthCheck.check();
        verifyAll();

        Assertions.assertFalse(result.isHealthy());
        Assertions.assertEquals("Only 3 of 111 stations have data, 40 entries present", result.getMessage());
    }

    @Test
    void shouldReportUnhealthyIfGoodEntriesButNoEnoughDueTrams() {
        LocalDate date = TestEnv.LocalNow().toLocalDate();
        LocalDateTime now = LocalDateTime.of(date, LocalTime.of(15,42));
        EasyMock.expect(providesNow.getDateTime()).andReturn(now);

        EasyMock.expect(repository.upToDateEntries()).andReturn(40);
        EasyMock.expect(stationRepository.getNumberOfStations(DataSourceID.tfgm, TransportMode.Tram)).andReturn(87L);
        EasyMock.expect(repository.getNumStationsWithData(now)).andReturn(83);
        EasyMock.expect(repository.getNumStationsWithTrams(now)).andReturn(3);

        replayAll();
        healthCheck.start();
        HealthCheck.Result result = healthCheck.check();
        verifyAll();

        Assertions.assertFalse(result.isHealthy());
        Assertions.assertEquals("Only 3 of 87 stations have due trams, 40 entries present", result.getMessage());
    }

    @Test
    void shouldNOTReportUnhealthyIfGoodEntriesButNoEnoughStationsLateNight() {
        LocalDateTime now = TramTime.of(4,0).toDate(TestEnv.testDay());
        EasyMock.expect(providesNow.getDateTime()).andReturn(now);

        EasyMock.expect(repository.upToDateEntries()).andReturn(40);
        EasyMock.expect(stationRepository.getNumberOfStations(DataSourceID.tfgm, TransportMode.Tram)).andReturn(111L);
        EasyMock.expect(repository.getNumStationsWithData(now)).andReturn(40);
        EasyMock.expect(repository.getNumStationsWithTrams(now)).andReturn(111);

        replayAll();
        healthCheck.start();
        HealthCheck.Result result = healthCheck.check();
        verifyAll();

        Assertions.assertTrue(result.isHealthy());
    }

    @Test
    void shouldNOTReportUnhealthyIfGoodEntriesButNotEnoughTramsLateNight() {
        LocalDateTime now = TramTime.of(4,0).toDate(TestEnv.testDay());
        EasyMock.expect(providesNow.getDateTime()).andReturn(now);

        EasyMock.expect(repository.upToDateEntries()).andReturn(40);
        EasyMock.expect(stationRepository.getNumberOfStations(DataSourceID.tfgm, TransportMode.Tram)).andReturn(111L);
        EasyMock.expect(repository.getNumStationsWithData(now)).andReturn(111);
        EasyMock.expect(repository.getNumStationsWithTrams(now)).andReturn(42);

        replayAll();
        healthCheck.start();
        HealthCheck.Result result = healthCheck.check();
        verifyAll();

        Assertions.assertTrue(result.isHealthy());
    }

}
