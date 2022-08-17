package com.tramchester.integration.repository;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.time.DateRange;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.RunningRoutesAndServices;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.reference.KnownTramRoute;
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
    private TramRouteHelper helper;

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
        helper = new TramRouteHelper();
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
    void shouldConsiderServicesFromDayBeforeIfTheyAreStillRunningTheFollowingDay() {
        LocalDate when = TestEnv.testDay();

        RunningRoutesAndServices.FilterForDate filter = runningRoutesAndServices.getFor(when);

        Route altyToBuryRoute = helper.getOneRoute(KnownTramRoute.AltrinchamManchesterBury, transportData, when);

        assertTrue(filter.isRouteRunning(altyToBuryRoute.getId(), false));
        assertTrue(filter.isRouteRunning(altyToBuryRoute.getId(), true));

        Set<Service> services = altyToBuryRoute.getServices();

        LocalDate previousDay = when.minusDays(1);

        Set<Service> allPreviousDay = services.stream().
                filter(service -> service.getCalendar().operatesOn(previousDay)).collect(Collectors.toSet());
        assertFalse(allPreviousDay.isEmpty());

        Set<Service> fromPreviousDay = allPreviousDay.stream().
                filter(service -> filter.isServiceRunningByTime(service.getId(), TramTime.of(0, 0), 25)).
                collect(Collectors.toSet());
        assertFalse(fromPreviousDay.isEmpty());

    }

    @Test
    void shouldConsiderRoutesFromDayBeforeIfTheyAreStillRunningTheFollowingDay() {
        LocalDate when = TestEnv.testDay();

        RunningRoutesAndServices.FilterForDate filter = runningRoutesAndServices.getFor(when);

        LocalDate previousDay = when.minusDays(1);

        Route altyToBuryRoute = helper.getOneRoute(KnownTramRoute.AltrinchamManchesterBury, transportData, previousDay);

        assertTrue(filter.isRouteRunning(altyToBuryRoute.getId(), false));
        assertTrue(filter.isRouteRunning(altyToBuryRoute.getId(), true));

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

        final LocalDate nextMonday = testDay.plusWeeks(1);

        // date range contains next monday and service does weekday operating
        List<Service> weekdayServices = transportData.getServices().stream().
                filter(service -> service.getCalendar().getDateRange().contains(nextMonday)).
                filter(service -> service.getCalendar().getOperatingDays().equals(weekdays)).
                collect(Collectors.toList());
        assertFalse(weekdayServices.isEmpty());

        // start of the range
        LocalDate weekdayServicesBegin = weekdayServices.stream().
                map(service -> service.getCalendar().getDateRange().getStartDate()).
                min(LocalDate::compareTo).get();

        // end of the range
        LocalDate weekdayServicesEnd = weekdayServices.stream().
                map(service -> service.getCalendar().getDateRange().getEndDate()).
                max(LocalDate::compareTo).get();

        DateRange weekdayDateRange = new DateRange(weekdayServicesBegin, weekdayServicesEnd);

        // double check contains next monday
        assertTrue(weekdayDateRange.contains(nextMonday));

        LocalDate friday = getFridayAfter(nextMonday);
        assertTrue(weekdayDateRange.contains(friday));

        RunningRoutesAndServices.FilterForDate filterForNextFriday = runningRoutesAndServices.getFor(friday);

        Set<Service> weekdayFiltered = weekdayServices.stream().
                filter(svc -> filterForNextFriday.isServiceRunningByDate(svc.getId(), false))
                .collect(Collectors.toSet());

        assertFalse(weekdayFiltered.isEmpty(), "Filter " + filterForNextFriday + " matched none of "
                + HasId.asIds(weekdayServices) + " testday: " + testDay);

        // services operating on a Saturday within this range
        List<Service> saturdayServices = transportData.getServices().stream().
                filter(service -> service.getCalendar().getOperatingDays().equals(saturdays)).
                filter(service -> weekdayDateRange.overlapsWith(service.getCalendar().getDateRange())).
                //filter(service -> service.getCalendar().overlapsDatesWith(weekdayDateRange)).
                //filter(service -> service.getCalendar().overlapsDatesAndDaysWith(weekdayDateRange, saturdays)).
                collect(Collectors.toList());
        assertFalse(saturdayServices.isEmpty(), weekdayDateRange.toString());

        Set<Service> matchingForSaturday = saturdayServices.stream().
                filter(service -> filterForNextFriday.isServiceRunningByDate(service.getId(), true)).
                collect(Collectors.toSet());

        // Most likely reason for fail here is a route switch over on this exact date; this tests selects
        // services that are running on the test day, which would definitely not be running the next day if there
        // is a cut-over that same day
        assertFalse(matchingForSaturday.isEmpty(), "Filter:" + filterForNextFriday + " not matching any of " +HasId.asIds(saturdayServices));

    }

    private LocalDate getFridayAfter(final LocalDate weekdayServicesBegin) {
        LocalDate result = weekdayServicesBegin;
        while (result.getDayOfWeek()!=FRIDAY) {
            result = result.plusDays(1);
        }
        return result;
    }


}
