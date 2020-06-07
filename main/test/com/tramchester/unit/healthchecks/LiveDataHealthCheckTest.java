package com.tramchester.unit.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.healthchecks.LiveDataHealthCheck;
import com.tramchester.repository.LiveDataRepository;
import com.tramchester.testSupport.TestEnv;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

class LiveDataHealthCheckTest extends EasyMockSupport {

    private LiveDataRepository repository;
    private LiveDataHealthCheck healthCheck;
    private TramTime now;

    @BeforeEach
    void beforeEachTestRuns() {
        now = TramTime.of(TestEnv.LocalNow());
        repository = createMock(LiveDataRepository.class);
        healthCheck = new LiveDataHealthCheck(repository, new ProvidesNow() {
            @Override
            public TramTime getNow() {
                return now;
            }

            @Override
            public LocalDate getDate() {
                return null;
            }

            @Override
            public LocalDateTime getDateTime() {
                return null;
            }
        });
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
    void shouldReportHealthyIfHaveDataAndNoStaleEntry() {
        EasyMock.expect(repository.upToDateEntries()).andReturn(40);
        EasyMock.expect(repository.missingDataCount()).andReturn(0L);
        EasyMock.expect(repository.upToDateEntries()).andReturn(40);

        replayAll();
        HealthCheck.Result result = healthCheck.check();
        verifyAll();

        Assertions.assertTrue(result.isHealthy());
    }

    @Test
    void shouldReportUnhealthIfStaleDate() {
        EasyMock.expect(repository.upToDateEntries()).andReturn(40);
        EasyMock.expect(repository.missingDataCount()).andReturn(2L);

        replayAll();
        HealthCheck.Result result = healthCheck.check();
        verifyAll();

        Assertions.assertFalse(result.isHealthy());
        Assertions.assertEquals("2 of 40 entries are stale", result.getMessage());
    }

    @Test
    void shouldReportUnhealthIfStaleTime() {
        EasyMock.expect(repository.upToDateEntries()).andReturn(40);
        EasyMock.expect(repository.upToDateEntries()).andReturn(20);
        EasyMock.expect(repository.missingDataCount()).andReturn(0L);

        replayAll();
        HealthCheck.Result result = healthCheck.check();
        verifyAll();

        Assertions.assertFalse(result.isHealthy());
        Assertions.assertEquals("20 of 40 entries are expired at "+now.toString(), result.getMessage());
    }
}
