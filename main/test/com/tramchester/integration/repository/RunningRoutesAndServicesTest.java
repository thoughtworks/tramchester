package com.tramchester.integration.repository;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.time.DateRange;
import com.tramchester.domain.time.TramServiceDate;
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

        Set<Route> routesFromTrips = tripsIntoNextDay.stream().map(Trip::getRoute).collect(Collectors.toSet());
        assertFalse(routesFromTrips.isEmpty());

        Set<Route> routesIntoNextDay = transportData.getRoutes().stream().filter(Route::intoNextDay).collect(Collectors.toSet());
        assertEquals(routesFromTrips, routesIntoNextDay);
    }

    @Test
    void shouldTakeAccountOfCrossingIntoNextDayForRunningServices() {
        // need to find service running mon to fri and one running saturday
        EnumSet<DayOfWeek> weekdays = EnumSet.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY);
        final EnumSet<DayOfWeek> saturdays = EnumSet.of(SATURDAY);

        LocalDate testDay = TestEnv.nextMonday();

        while (new TramServiceDate(testDay).isChristmasPeriod()) {
            testDay = testDay.plusWeeks(1);
        }

        final LocalDate nextMonday = testDay;

        List<Service> weekdayServices = transportData.getServices().stream().
                filter(service -> service.getCalendar().getDateRange().contains(nextMonday)).
                filter(service -> service.getCalendar().getOperatingDays().equals(weekdays)).
                collect(Collectors.toList());
        assertFalse(weekdayServices.isEmpty());

        LocalDate weekdayServicesBegin = weekdayServices.stream().
                map(service -> service.getCalendar().getDateRange().getStartDate()).
                min(LocalDate::compareTo).get();

        LocalDate weekdayServicesEnd = weekdayServices.stream().
                map(service -> service.getCalendar().getDateRange().getEndDate()).
                max(LocalDate::compareTo).get();

        DateRange weekdayDateRange = new DateRange(weekdayServicesBegin, weekdayServicesEnd);
        assertTrue(weekdayDateRange.contains(nextMonday));

        List<Service> saturdayServices = transportData.getServices().stream().
                filter(service -> service.getCalendar().getOperatingDays().equals(saturdays)).
                filter(service -> service.getCalendar().overlapsDatesAndDaysWith(weekdayDateRange, saturdays)).
                collect(Collectors.toList());
        assertFalse(saturdayServices.isEmpty(), weekdayDateRange.toString());

//        int offsetToFriday = 1;
//        while (nextMonday.plusDays(offsetToFriday).getDayOfWeek()!=FRIDAY) {
//            offsetToFriday++;
//        }

        LocalDate friday = TestEnv.nextSaturday().minusDays(1); // weekdayServicesBegin.plusDays(offsetToFriday);
        assertTrue(weekdayDateRange.contains(friday));

        RunningRoutesAndServices.FilterForDate filter = runningRoutesAndServices.getFor(friday);

        Set<Service> weekdayFiltered = weekdayServices.stream().
                filter(svc -> filter.isServiceRunningByDate(svc.getId(), false))
                .collect(Collectors.toSet());

        assertFalse(weekdayFiltered.isEmpty(), "Filter " + filter + " matched none of "
                + weekdayServices + " testday: " + testDay);

        final Service saturdayService = saturdayServices.get(0);

        // not including next day
        assertFalse(filter.isServiceRunningByDate(saturdayService.getId(), false));

        // including next day
        assertTrue(filter.isServiceRunningByDate(saturdayService.getId(), true));

    }
}
