package com.tramchester.unit.domain;

import com.tramchester.domain.time.TramTime;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.RunningRoutesAndServices;
import com.tramchester.repository.ServiceRepository;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static com.tramchester.domain.id.IdSet.singleton;
import static com.tramchester.domain.id.StringIdFor.createId;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RunningRoutesAndServicesTest extends EasyMockSupport {

    private LocalDate date;
    private ServiceRepository serviceRepository;
    private RouteRepository routeRepository;
    private RunningRoutesAndServices runningRoutesAndServices;

    @BeforeEach
    void beforeEachTestRuns() {
        date = LocalDate.of(2021, 12, 5);
        serviceRepository = createMock(ServiceRepository.class);
        routeRepository = createMock(RouteRepository.class);

        runningRoutesAndServices = new RunningRoutesAndServices(serviceRepository, routeRepository);
    }

    @Test
    void shouldHaveRunning() {

        EasyMock.expect(routeRepository.getRoutesRunningOn(date)).andReturn(singleton(createId("routeAId")));
        EasyMock.expect(serviceRepository.getServicesOnDate(date)).andReturn(singleton(createId("serviceAId")));

        LocalDate tomorrow = date.plusDays(1);
        EasyMock.expect(routeRepository.getRoutesRunningOn(tomorrow)).andReturn(singleton(createId("routeBId")));
        EasyMock.expect(serviceRepository.getServicesOnDate(tomorrow)).andReturn(singleton(createId("serviceBId")));

        replayAll();
        RunningRoutesAndServices.FilterForDate filter = runningRoutesAndServices.getFor(date);
        verifyAll();

        TramTime time = TramTime.of(10,45);
        assertFalse(filter.isRouteRunning(createId("routeBId"), time));
        assertTrue(filter.isRouteRunning(createId("routeAId"), time));

        assertFalse(filter.isServiceRunning(createId("serviceBId"), time));
        assertTrue(filter.isServiceRunning(createId("serviceAId"), time));
    }

    @Test
    void shouldIncludeFollowingDayIfTramTimeIsNextDay() {

        EasyMock.expect(routeRepository.getRoutesRunningOn(date)).andReturn(singleton(createId("routeAId")));
        EasyMock.expect(serviceRepository.getServicesOnDate(date)).andReturn(singleton(createId("serviceAId")));

        LocalDate tomorrow = date.plusDays(1);
        EasyMock.expect(routeRepository.getRoutesRunningOn(tomorrow)).andReturn(singleton(createId("routeBId")));
        EasyMock.expect(serviceRepository.getServicesOnDate(tomorrow)).andReturn(singleton(createId("serviceBId")));

        replayAll();
        RunningRoutesAndServices.FilterForDate filter = runningRoutesAndServices.getFor(date);
        verifyAll();

        TramTime time = TramTime.of(23,45);

        assertTrue(filter.isRouteRunning(createId("routeAId"), time));
        assertTrue(filter.isServiceRunning(createId("serviceAId"), time));

        assertFalse(filter.isRouteRunning(createId("routeBId"), time));
        assertFalse(filter.isServiceRunning(createId("serviceBId"), time));

        TramTime nextDay = TramTime.nextDay(0,10);
        assertTrue(filter.isRouteRunning(createId("routeAId"), nextDay));
        assertTrue(filter.isServiceRunning(createId("serviceAId"), nextDay));

        assertTrue(filter.isRouteRunning(createId("routeBId"), nextDay));
        assertTrue(filter.isServiceRunning(createId("serviceBId"), nextDay));
    }

}
