package com.tramchester.unit.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import com.tramchester.domain.ServiceTimeLimits;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.healthchecks.CachingHealthCheck;
import com.tramchester.healthchecks.TramchesterHealthCheck;
import com.tramchester.testSupport.TestEnv;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CachingHealthCheckTest extends EasyMockSupport {

    private LocalDateTime time;
    private ProvidesLocalNow providesLocalNow;
    private TramchesterHealthCheck containedCheck;
    private Duration cacheDuration;

    @BeforeEach
    void beforeEachTest() {
        LocalDate date = TestEnv.LocalNow().toLocalDate();
        time = LocalDateTime.of(date, LocalTime.of(15,42));
        providesLocalNow = createMock(ProvidesLocalNow.class);
        containedCheck = createMock(TramchesterHealthCheck.class);

        cacheDuration = Duration.ofSeconds(3);

        ServiceTimeLimits serviceTimeLimits = new ServiceTimeLimits();

        EasyMock.expect(containedCheck.getName()).andStubReturn("containedName");
        EasyMock.expect(containedCheck.getServiceTimeLimits()).andStubReturn(serviceTimeLimits);


    }

    @Test
    void shouldUseCached() {

        EasyMock.expect(providesLocalNow.getDateTime()).andReturn(time);
        EasyMock.expect(containedCheck.execute()).andReturn(HealthCheck.Result.healthy());
        EasyMock.expect(providesLocalNow.getDateTime()).andReturn(time);

        replayAll();
        CachingHealthCheck healthCheck = new CachingHealthCheck(containedCheck, cacheDuration, providesLocalNow);
        HealthCheck.Result result = healthCheck.execute();
        healthCheck.execute();
        verifyAll();

        assertTrue(result.isHealthy());

    }

    @Test
    void shouldUseRefresh() {

        EasyMock.expect(providesLocalNow.getDateTime()).andReturn(time);
        EasyMock.expect(containedCheck.execute()).andReturn(HealthCheck.Result.healthy());

        EasyMock.expect(providesLocalNow.getDateTime()).andReturn(time.plus(cacheDuration).plusSeconds(1));
        EasyMock.expect(containedCheck.execute()).andReturn(HealthCheck.Result.unhealthy("a message"));

        replayAll();
        CachingHealthCheck healthCheck = new CachingHealthCheck(containedCheck, cacheDuration, providesLocalNow);
        HealthCheck.Result resultA = healthCheck.execute();
        HealthCheck.Result resultB = healthCheck.execute();
        verifyAll();

        assertTrue(resultA.isHealthy());
        assertFalse(resultB.isHealthy());


    }

}
