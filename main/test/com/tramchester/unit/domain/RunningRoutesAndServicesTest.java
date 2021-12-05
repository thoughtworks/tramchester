package com.tramchester.unit.domain;

import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.RunningRoutesAndServices;
import com.tramchester.repository.ServiceRepository;
import com.tramchester.repository.TripRepository;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RunningRoutesAndServicesTest extends EasyMockSupport {

    @Test
    void shouldHaveRunning() {
        TramServiceDate date = TramServiceDate.of(LocalDate.of(2021, 12, 5));

        ServiceRepository serviceRepository = createMock(ServiceRepository.class);
        RouteRepository routeRepository = createMock(RouteRepository.class);
        TripRepository tripRepository = createMock(TripRepository.class);

        IdSet<Route> routeIds = IdSet.singleton(StringIdFor.createId("routeAId"));
        IdSet<Service> serviceIds = IdSet.singleton(StringIdFor.createId("serviceAId"));

        EasyMock.expect(routeRepository.getRoutesRunningOn(date)).andReturn(routeIds);
        EasyMock.expect(serviceRepository.getServicesOnDate(date)).andReturn(serviceIds);

        replayAll();

        RunningRoutesAndServices runningRoutesAndServices =
                new RunningRoutesAndServices(serviceRepository, routeRepository, tripRepository);

        RunningRoutesAndServices.FilterForDate filter = runningRoutesAndServices.getFor(date);
        verifyAll();

        assertFalse(filter.isRouteRunning(StringIdFor.createId("routeBId")));
        assertTrue(filter.isRouteRunning(StringIdFor.createId("routeAId")));

        assertFalse(filter.isServiceRunning(StringIdFor.createId("serviceBId")));
        assertTrue(filter.isServiceRunning(StringIdFor.createId("serviceAId")));

    }
}
