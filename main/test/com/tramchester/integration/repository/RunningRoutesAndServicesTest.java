package com.tramchester.integration.repository;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.time.DateRange;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.RunningRoutesAndServices;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.time.DayOfWeek.*;
import static org.junit.jupiter.api.Assertions.*;

public class RunningRoutesAndServicesTest {

    private static ComponentContainer componentContainer;
    private TransportData transportData;
    private RunningRoutesAndServices runningRoutesAndServices;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        TramchesterConfig config = new IntegrationTramTestConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        transportData = componentContainer.get(TransportData.class);
        runningRoutesAndServices = componentContainer.get(RunningRoutesAndServices.class);
    }

    @Test
    void shouldHaveTripsServicesAndRoutesThatCrossIntoNextDay() {

        Set<Trip> tripsIntoNextDay = transportData.getTrips().stream().filter(Trip::intoNextDay).collect(Collectors.toSet());

        assertFalse(tripsIntoNextDay.isEmpty());

        Set<Service> servicesFromTrips = tripsIntoNextDay.stream().map(Trip::getService).collect(Collectors.toSet());

        servicesFromTrips.forEach(service -> assertTrue(runningRoutesAndServices.intoNextDay(service.getId()),
                service.getId() + " should be into next day"));

        Set<Service> servicesIntoNextDay = transportData.getServices().stream().
                filter(service -> runningRoutesAndServices.intoNextDay(service.getId())).collect(Collectors.toSet());

        assertEquals(servicesIntoNextDay, servicesFromTrips);

        Set<Route> routesFromTrips = tripsIntoNextDay.stream().map(Trip::getRoute).collect(Collectors.toSet());
        assertFalse(routesFromTrips.isEmpty());

        Set<Route> routesIntoNextDay = transportData.getRoutes().stream().filter(Route::intoNextDay).collect(Collectors.toSet());
        assertEquals(routesFromTrips, routesIntoNextDay);
    }

    @Test
    void shouldTakeAccountOfCrossingIntoNextDayForRunningServices() {
        // need to find service running mon to fri and one running saturday
        EnumSet<DayOfWeek> weekday = EnumSet.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY);
        final EnumSet<DayOfWeek> saturdays = EnumSet.of(SATURDAY);

        LocalDate monday = TestEnv.nextMonday();
        while (new TramServiceDate(monday).isChristmasPeriod()) {
            monday = monday.plusWeeks(1);
        }

        final LocalDate testDay = monday.plusWeeks(1);

        List<Service> weekdayServices = transportData.getServices().stream().
                filter(service -> service.getCalendar().getDateRange().contains(testDay)).
                filter(service -> service.getCalendar().getOperatingDays().equals(weekday)).
                collect(Collectors.toList());
        assertFalse(weekdayServices.isEmpty());

        LocalDate weekdayServicesBegin = weekdayServices.stream().
                map(service -> service.getCalendar().getDateRange().getStartDate()).min(LocalDate::compareTo).get();
        LocalDate weekdayServicesEnd = weekdayServices.stream().
                map(service -> service.getCalendar().getDateRange().getEndDate()).max(LocalDate::compareTo).get();

        DateRange weekdayDateRange = new DateRange(weekdayServicesBegin, weekdayServicesEnd);
        assertTrue(weekdayDateRange.contains(testDay));

        List<Service> saturdayServices = transportData.getServices().stream().
                filter(service -> service.getCalendar().getOperatingDays().equals(saturdays)).
                filter(service -> service.getCalendar().overlapsDatesAndDaysWith(weekdayDateRange, saturdays)).
                collect(Collectors.toList());
        assertFalse(saturdayServices.isEmpty());

        int offset = 1;
        while (weekdayServicesBegin.plusDays(offset).getDayOfWeek()!=FRIDAY) {
            offset++;
        }
        LocalDate friday = weekdayServicesBegin.plusDays(offset);
        assertTrue(weekdayDateRange.contains(friday));

        RunningRoutesAndServices.FilterForDate filter = runningRoutesAndServices.getFor(friday);

        final Service weekdayService = weekdayServices.get(0);
        final Service saturdayService = saturdayServices.get(0);

        assertTrue(filter.isServiceRunning(weekdayService.getId(), TramTime.of(23,12)));
        assertFalse(filter.isServiceRunning(saturdayService.getId(), TramTime.of(23,12)));
        assertTrue(filter.isServiceRunning(saturdayService.getId(), TramTime.nextDay(0,15)));

    }
}
