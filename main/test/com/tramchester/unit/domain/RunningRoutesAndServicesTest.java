package com.tramchester.unit.domain;

import com.tramchester.domain.MutableService;
import com.tramchester.domain.Service;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.time.TramTime;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.RunningRoutesAndServices;
import com.tramchester.repository.ServiceRepository;
import com.tramchester.repository.TripRepository;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.tramchester.domain.id.IdSet.singleton;
import static com.tramchester.domain.id.StringIdFor.createId;
import static org.junit.jupiter.api.Assertions.*;

public class RunningRoutesAndServicesTest extends EasyMockSupport {

    private LocalDate date;
    private ServiceRepository serviceRepository;
    private RouteRepository routeRepository;
    private TripRepository tripRepository;
    private RunningRoutesAndServices runningRoutesAndServices;

    @BeforeEach
    void beforeEachTestRuns() {
        date = LocalDate.of(2021, 12, 5);
        serviceRepository = createMock(ServiceRepository.class);
        routeRepository = createMock(RouteRepository.class);
        tripRepository = createMock(TripRepository.class);

        runningRoutesAndServices =
                new RunningRoutesAndServices(serviceRepository, routeRepository, tripRepository);
    }

    @Test
    void shouldHaveRunning() {

        EasyMock.expect(tripRepository.getTrips()).andReturn(Collections.emptySet());

        EasyMock.expect(routeRepository.getRoutesRunningOn(date)).andReturn(singleton(createId("routeAId")));
        EasyMock.expect(serviceRepository.getServicesOnDate(date)).andReturn(singleton(createId("serviceAId")));

        LocalDate tomorrow = date.plusDays(1);
        EasyMock.expect(routeRepository.getRoutesRunningOn(tomorrow)).andReturn(singleton(createId("routeBId")));
        EasyMock.expect(serviceRepository.getServicesOnDate(tomorrow)).andReturn(singleton(createId("serviceBId")));

        replayAll();
        runningRoutesAndServices.start();
        RunningRoutesAndServices.FilterForDate filter = runningRoutesAndServices.getFor(date);
        verifyAll();

        TramTime time = TramTime.of(10,45);
        assertFalse(filter.isRouteRunning(createId("routeBId"), time));
        assertTrue(filter.isRouteRunning(createId("routeAId"), time));

        assertFalse(filter.isServiceRunning(createId("serviceBId"), time));
        assertTrue(filter.isServiceRunning(createId("serviceAId"), time));
    }

    @Test
    void shouldHaveServicesIntoNextDay() {

        Service serviceB = MutableService.build(createId("serviceB1"));

        Trip tripA = createMock(Trip.class);
        Trip tripB = createMock(Trip.class);
        Set<Trip> trips = new HashSet<>(Arrays.asList(tripA, tripB));
        EasyMock.expect(tripRepository.getTrips()).andReturn(trips);

        EasyMock.expect(tripA.intoNextDay()).andReturn(false);
        EasyMock.expect(tripB.intoNextDay()).andReturn(true);
        EasyMock.expect(tripB.getService()).andReturn(serviceB);

        replayAll();
        runningRoutesAndServices.start();
        assertFalse(runningRoutesAndServices.intoNextDay(createId("serviceA1")));
        assertTrue(runningRoutesAndServices.intoNextDay(createId("serviceB1")));
        verifyAll();
    }

    @Test
    void shouldIncludeFollowingDayIfTramTimeIsNextDay() {

        EasyMock.expect(tripRepository.getTrips()).andReturn(Collections.emptySet());

        EasyMock.expect(routeRepository.getRoutesRunningOn(date)).andReturn(singleton(createId("routeAId")));
        EasyMock.expect(serviceRepository.getServicesOnDate(date)).andReturn(singleton(createId("serviceAId")));

        LocalDate tomorrow = date.plusDays(1);
        EasyMock.expect(routeRepository.getRoutesRunningOn(tomorrow)).andReturn(singleton(createId("routeBId")));
        EasyMock.expect(serviceRepository.getServicesOnDate(tomorrow)).andReturn(singleton(createId("serviceBId")));

        replayAll();
        runningRoutesAndServices.start();
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
