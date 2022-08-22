package com.tramchester.unit.domain;

import com.tramchester.domain.*;
import com.tramchester.domain.dates.MutableServiceCalendar;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.input.MutableTrip;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.testSupport.TestEnv;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

import static com.tramchester.domain.id.StringIdFor.createId;
import static com.tramchester.domain.reference.TransportMode.Tram;
import static java.time.DayOfWeek.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteTest {

    // TODO A lot of the date range tests here could go against the aggregate calendar

    private EnumSet<DayOfWeek> NO_DAYS;
    private EnumSet<DayOfWeek> ALL_DAYS;
    private IdFor<Route> routeIdA;
    private IdFor<Route> routeIdB;
    private IdFor<Route> routeId;
    private IdFor<Service> serviceId;
    private IdFor<Service> serviceIdA;
    private IdFor<Service> serviceIdB;

    @BeforeEach
    void setUp() {
        NO_DAYS = EnumSet.noneOf(DayOfWeek.class);
        ALL_DAYS = EnumSet.allOf(DayOfWeek.class);
        routeId = createId("routeId");
        routeIdA = createId("routeIdA");
        routeIdB = createId("routeIdB");
        serviceId = StringIdFor.createId("serviceId");
        serviceIdA = StringIdFor.createId("serviceIdA");
        serviceIdB = StringIdFor.createId("serviceIdB");
    }

    @Test
    void shouldHaveTramRoute() {
        Route route = MutableRoute.getRoute(createId("idA"),"code","name", TestEnv.MetAgency(),
                Tram);
        Assertions.assertTrue(TransportMode.isTram(route));

        final Agency agency = MutableAgency.build(DataSourceID.tfgm, createId("GMS"), "agencyName");
        route = MutableRoute.getRoute(createId("idB"),"code","name",
                agency, TransportMode.Bus);
        assertFalse(TransportMode.isTram(route));
    }

    @Test
    void shouldAddService() {
        LocalDate startDate = LocalDate.of(2020, 11, 5);
        LocalDate endDate = LocalDate.of(2020, 11, 25);

        MutableRoute route = createRoute(routeId, "code", "name");

        route.addService(createService(startDate, endDate,"serviceId", EnumSet.of(MONDAY)));
        route.addService(createService(startDate, endDate,"serviceId", EnumSet.of(MONDAY)));
        route.addService(createService(startDate, endDate,"serviceId2", EnumSet.of(MONDAY)));

        Set<Service> services = route.getServices();

        Assertions.assertEquals(2, services.size());
    }

    @Test
    void shouldAddTrip() {
        LocalDate startDate = LocalDate.of(2020, 11, 5);
        LocalDate endDate = LocalDate.of(2020, 11, 25);

        MutableRoute route = createRoute(routeId, "code", "name");

        final Service serviceA = createService(startDate, endDate, "serviceId", EnumSet.of(MONDAY));

        route.addTrip(new MutableTrip(StringIdFor.createId("tripA"), "headSignA", serviceA, route, Tram));
        route.addTrip(new MutableTrip(StringIdFor.createId("tripB"), "headSignB", serviceA, route, Tram));

        Set<Trip> trips = route.getTrips();

        Assertions.assertEquals(2, trips.size());
    }

    @Test
    void shouldRespectDateRangesOnService() {
        final LocalDate startDate = LocalDate.of(2020, 11, 5);
        final LocalDate endDate = LocalDate.of(2020, 11, 25);

        MutableRoute route = createRoute(routeId, "code", "name");

        MutableService service = new MutableService(serviceId);
        MutableServiceCalendar calendar = new MutableServiceCalendar(DateRange.of(startDate, endDate), ALL_DAYS);
        service.setCalendar(calendar);

        route.addService(service);

        assertFalse(route.isAvailableOn(startDate.minusDays(1)));
        assertFalse(route.isAvailableOn(endDate.plusDays(1)));

        LocalDate date = startDate;
        while (date.isBefore(endDate)) {
            assertTrue(service.getCalendar().operatesOn(date));
            assertTrue(route.isAvailableOn(date), "should be available on " + date);
            date = date.plusDays(1);
        }

    }

    @Test
    void shouldRespectDateRangesOnServicesNoOverlap() {
        final LocalDate startDateA = LocalDate.of(2020, 11, 5);
        final LocalDate endDateA = LocalDate.of(2020, 11, 25);

        final LocalDate startDateB = LocalDate.of(2020, 12, 5);
        final LocalDate endDateB = LocalDate.of(2020, 12, 25);

        MutableRoute route = createRoute(routeId, "code", "name");

        MutableService serviceA = new MutableService(serviceIdA);
        MutableServiceCalendar calendarA = new MutableServiceCalendar(DateRange.of(startDateA, endDateA), ALL_DAYS);
        serviceA.setCalendar(calendarA);

        MutableService serviceB = new MutableService(serviceIdB);
        MutableServiceCalendar calendarB = new MutableServiceCalendar(DateRange.of(startDateB, endDateB), ALL_DAYS);
        serviceB.setCalendar(calendarB);

        route.addService(serviceA);
        route.addService(serviceB);

        assertFalse(route.isAvailableOn(startDateA.minusDays(1)));
        assertFalse(route.isAvailableOn(endDateA.plusDays(1)));

        assertFalse(route.isAvailableOn(startDateB.minusDays(1)));
        assertFalse(route.isAvailableOn(endDateB.plusDays(1)));

        LocalDate date = startDateA;
        while (date.isBefore(endDateA)) {
            assertTrue(route.isAvailableOn(date), "should be available on " + date);
            date = date.plusDays(1);
        }

        date = startDateB;
        while (date.isBefore(endDateB)) {
            assertTrue(route.isAvailableOn(date), "should be available on " + date);
            date = date.plusDays(1);
        }

    }

    @Test
    void shouldRespectDateRangesOnServicesWithOverlap() {
        final LocalDate startDateA = LocalDate.of(2020, 11, 5);
        final LocalDate endDateA = LocalDate.of(2020, 11, 25);

        final LocalDate startDateB = endDateA.minusDays(1);
        final LocalDate endDateB = LocalDate.of(2020, 12, 25);

        MutableRoute route = createRoute(routeId, "code", "name");

        MutableService serviceA = new MutableService(serviceIdA);
        MutableServiceCalendar calendarA = new MutableServiceCalendar(DateRange.of(startDateA, endDateA), ALL_DAYS);
        serviceA.setCalendar(calendarA);

        MutableService serviceB = new MutableService(serviceIdB);
        MutableServiceCalendar calendarB = new MutableServiceCalendar(DateRange.of(startDateB, endDateB), ALL_DAYS);
        serviceB.setCalendar(calendarB);

        route.addService(serviceA);
        route.addService(serviceB);

        assertFalse(route.isAvailableOn(startDateA.minusDays(1)));
        assertFalse(route.isAvailableOn(endDateB.plusDays(1)));

        LocalDate date = startDateA;
        while (date.isBefore(endDateB)) {
            assertTrue(route.isAvailableOn(date), "should be available on " + date);
            date = date.plusDays(1);
        }

    }

    // See also AggregateServiceCalendarTest

    @Test
    void shouldRespectDateRangesOnServicesWithNoAdditionalDayOverlap() {
        final LocalDate startDate = LocalDate.of(2020, 11, 5);
        final LocalDate endDate = LocalDate.of(2020, 11, 25);

        MutableRoute routeA = createRoute(routeIdA, "code", "nameA");
        MutableRoute routeB = createRoute(routeIdB, "code", "nameB");

        MutableService serviceA = new MutableService(serviceIdA);
        MutableServiceCalendar calendarA = new MutableServiceCalendar(DateRange.of(startDate, endDate), ALL_DAYS);
        serviceA.setCalendar(calendarA);

        MutableService serviceB = new MutableService(serviceIdB);
        MutableServiceCalendar calendarB = new MutableServiceCalendar(DateRange.of(startDate, endDate), NO_DAYS);
        serviceB.setCalendar(calendarB);

        routeA.addService(serviceA);
        routeB.addService(serviceB);

        assertFalse(routeA.isDateOverlap(routeB));
        assertFalse(routeB.isDateOverlap(routeA));

    }

    @Test
    void shouldRespectDateRangesOnServicesWithAdditionalDayOverlap() {
        final LocalDate startDate = LocalDate.of(2020, 11, 5);
        final LocalDate endDate = LocalDate.of(2020, 11, 25);


        MutableRoute routeA = createRoute(routeIdA, "code", "nameA");
        MutableRoute routeB = createRoute(routeIdB, "code", "nameB");

        MutableService serviceA = new MutableService(serviceIdA);
        MutableServiceCalendar calendarA = new MutableServiceCalendar(DateRange.of(startDate, endDate), ALL_DAYS);
        serviceA.setCalendar(calendarA);

        MutableService serviceB = new MutableService(serviceIdB);
        MutableServiceCalendar calendarB = new MutableServiceCalendar(DateRange.of(startDate, endDate), NO_DAYS);
        calendarB.includeExtraDate(startDate.plusDays(1));
        serviceB.setCalendar(calendarB);

        routeA.addService(serviceA);
        routeB.addService(serviceB);

        assertTrue(routeA.isDateOverlap(routeB));
        assertTrue(routeB.isDateOverlap(routeA));

    }

    @Test
    void shouldRespectDateRangesOnServicesWithAdditionalAndExcludeNegatingAnyOverlap() {
        final LocalDate startDate = LocalDate.of(2020, 11, 5);
        final LocalDate endDate = LocalDate.of(2020, 11, 25);

        LocalDate specialDay = LocalDate.of(2020,11,15);

        DateRange dateRange = DateRange.of(startDate, endDate);

        MutableRoute routeA = createRoute(routeIdA, "code", "nameA");
        MutableRoute routeB = createRoute(routeIdB, "code", "nameB");

        MutableService serviceA = new MutableService(serviceIdA);
        MutableServiceCalendar calendarA = new MutableServiceCalendar(dateRange, ALL_DAYS);
        calendarA.excludeDate(specialDay); // exclude
        serviceA.setCalendar(calendarA);

        MutableService serviceB = new MutableService(serviceIdB);
        MutableServiceCalendar calendarB = new MutableServiceCalendar(dateRange, NO_DAYS);
        calendarB.includeExtraDate(specialDay); // include
        serviceB.setCalendar(calendarB);

        routeA.addService(serviceA);
        routeB.addService(serviceB);

        assertFalse(routeA.isDateOverlap(routeB));
        assertFalse(routeB.isDateOverlap(routeA));

    }

    @NotNull
    private MutableRoute createRoute(IdFor<Route> routeIdA, String code, String nameA) {
        return new MutableRoute(routeIdA, code, nameA, TestEnv.MetAgency(),
                Tram);
    }

    @Test
    void shouldRespectDateRangesOnServicesNoOverlapExcludedDays() {
        final LocalDate startDateA = LocalDate.of(2020, 11, 5);
        final LocalDate endDateA = LocalDate.of(2020, 11, 25);

        final LocalDate startDateB = LocalDate.of(2020, 12, 5);
        final LocalDate endDateB = LocalDate.of(2020, 12, 25);

        MutableRoute route = createRoute(routeId, "code", "name");

        MutableService serviceA = new MutableService(serviceIdA);
        MutableServiceCalendar calendarA = new MutableServiceCalendar(DateRange.of(startDateA, endDateA), ALL_DAYS);
        LocalDate exclusionOne = LocalDate.of(2020, 11, 10);
        calendarA.excludeDate(exclusionOne);
        serviceA.setCalendar(calendarA);

        MutableService serviceB = new MutableService(serviceIdB);
        MutableServiceCalendar calendarB = new MutableServiceCalendar(DateRange.of(startDateB, endDateB), ALL_DAYS);
        LocalDate exclusionTwo = LocalDate.of(2020, 12, 10);
        calendarB.excludeDate(exclusionTwo);
        serviceB.setCalendar(calendarB);

        route.addService(serviceA);
        route.addService(serviceB);

        assertFalse(route.isAvailableOn(exclusionOne));
        assertFalse(route.isAvailableOn(exclusionTwo));
    }

    @Test
    void shouldRespectDateRangesOnServicesIncludedDays() {
        final LocalDate startDate = LocalDate.of(2020, 11, 5);
        final LocalDate endDate = LocalDate.of(2020, 11, 25);

        DateRange dateRange = DateRange.of(startDate, endDate);
        EnumSet<DayOfWeek> noDays = NO_DAYS;

        MutableRoute route = createRoute(routeId, "code", "name");

        MutableService serviceA = new MutableService(serviceIdA);

        MutableServiceCalendar calendarA = new MutableServiceCalendar(dateRange, noDays);
        LocalDate additionOne = LocalDate.of(2020, 11, 10);
        calendarA.includeExtraDate(additionOne);
        serviceA.setCalendar(calendarA);

        MutableService serviceB = new MutableService(serviceIdB);
        MutableServiceCalendar calendarB = new MutableServiceCalendar(dateRange, noDays);
        LocalDate additionTwo = LocalDate.of(2020, 11, 12);
        calendarB.includeExtraDate(additionTwo);
        serviceB.setCalendar(calendarB);

        route.addService(serviceA);
        route.addService(serviceB);

        assertTrue(route.isAvailableOn(additionOne));
        assertTrue(route.isAvailableOn(additionTwo));

        dateRange.stream().filter(date -> !(date.equals(additionOne) || date.equals(additionTwo))).
                forEach(date -> assertFalse(route.isAvailableOn(date), "should not be availble on " + date));

    }

    @Test
    void shouldRespectDateRangesOverlapDifferentAdditionalDays() {
        final LocalDate startDate = LocalDate.of(2020, 11, 5);
        final LocalDate endDate = LocalDate.of(2020, 11, 25);

        DateRange dateRange = DateRange.of(startDate, endDate);
        EnumSet<DayOfWeek> noDays = NO_DAYS;

        MutableRoute routeA = createRoute(routeIdA, "code", "nameA");
        MutableRoute routeB = createRoute(routeIdB, "code", "nameA");

        MutableService serviceA = new MutableService(serviceIdA);

        MutableServiceCalendar calendarA = new MutableServiceCalendar(dateRange, noDays);
        LocalDate additionOne = LocalDate.of(2020, 11, 10);
        calendarA.includeExtraDate(additionOne);
        serviceA.setCalendar(calendarA);

        MutableService serviceB = new MutableService(serviceIdB);
        MutableServiceCalendar calendarB = new MutableServiceCalendar(dateRange, noDays);
        LocalDate additionTwo = LocalDate.of(2020, 11, 12);
        calendarB.includeExtraDate(additionTwo);
        serviceB.setCalendar(calendarB);

        routeA.addService(serviceA);
        routeB.addService(serviceB);

        assertFalse(routeA.isDateOverlap(routeB));
        assertFalse(routeB.isDateOverlap(routeA));
    }


    @Test
    void shouldRespectDateRangesOverlapSameAdditionalDays() {
        final LocalDate startDate = LocalDate.of(2020, 11, 5);
        final LocalDate endDate = LocalDate.of(2020, 11, 25);

        DateRange dateRange = DateRange.of(startDate, endDate);
        EnumSet<DayOfWeek> noDays = NO_DAYS;

        MutableRoute routeA = createRoute(routeIdA, "code", "nameA");
        MutableRoute routeB = createRoute(routeIdB, "code", "nameA");

        MutableService serviceA = new MutableService(serviceIdA);

        MutableServiceCalendar calendarA = new MutableServiceCalendar(dateRange, noDays);
        LocalDate addition = LocalDate.of(2020, 11, 10);
        calendarA.includeExtraDate(addition);
        serviceA.setCalendar(calendarA);

        MutableService serviceB = new MutableService(serviceIdB);
        MutableServiceCalendar calendarB = new MutableServiceCalendar(dateRange, noDays);
        calendarB.includeExtraDate(addition);
        serviceB.setCalendar(calendarB);

        routeA.addService(serviceA);
        routeB.addService(serviceB);

        assertTrue(routeA.isDateOverlap(routeB));
        assertTrue(routeB.isDateOverlap(routeA));
    }

    @Test
    void shouldRespectDateRangesOverlapNotSameWeekdaysWithAddition() {
        final LocalDate startDateA = LocalDate.of(2020, 11, 5);
        final LocalDate endDateA = LocalDate.of(2020, 11, 25);

        DateRange dateRange = DateRange.of(startDateA, endDateA);

        MutableRoute routeA = createRoute(routeIdA, "code", "nameA");
        MutableRoute routeB = createRoute(routeIdB, "code", "nameA");

        MutableService serviceA = new MutableService(serviceIdA);

        MutableServiceCalendar calendarA = new MutableServiceCalendar(dateRange, EnumSet.of(MONDAY, TUESDAY));
        LocalDate addition = LocalDate.of(2020, 11, 10);
        calendarA.includeExtraDate(addition);
        serviceA.setCalendar(calendarA);

        MutableService serviceB = new MutableService(serviceIdB);
        MutableServiceCalendar calendarB = new MutableServiceCalendar(dateRange, EnumSet.of(WEDNESDAY, THURSDAY));
        calendarB.includeExtraDate(addition);
        serviceB.setCalendar(calendarB);

        routeA.addService(serviceA);
        routeB.addService(serviceB);

        assertTrue(routeA.isDateOverlap(routeB));
        assertTrue(routeB.isDateOverlap(routeA));
    }

    @Test
    void shouldRespectDateRangesOverlapNotSameWeekdaysWithOutAddition() {
        final LocalDate startDateA = LocalDate.of(2020, 11, 5);
        final LocalDate endDateA = LocalDate.of(2020, 11, 25);

        DateRange dateRange = DateRange.of(startDateA, endDateA);

        MutableRoute routeA = createRoute(routeIdA, "code", "nameA");
        MutableRoute routeB = createRoute(routeIdB, "code", "nameA");

        MutableService serviceA = new MutableService(serviceIdA);

        MutableServiceCalendar calendarA = new MutableServiceCalendar(dateRange, EnumSet.of(MONDAY, TUESDAY));
        serviceA.setCalendar(calendarA);

        MutableService serviceB = new MutableService(serviceIdB);
        MutableServiceCalendar calendarB = new MutableServiceCalendar(dateRange, EnumSet.of(WEDNESDAY, THURSDAY));
        serviceB.setCalendar(calendarB);

        routeA.addService(serviceA);
        routeB.addService(serviceB);

        assertFalse(routeA.isDateOverlap(routeB));
        assertFalse(routeB.isDateOverlap(routeA));
    }

    @Test
    void shouldRespectDateRangesOnServicesInclusionOverridesExclusion() {
        final LocalDate startDateA = LocalDate.of(2020, 11, 5);
        final LocalDate endDateA = LocalDate.of(2020, 11, 25);

        MutableRoute route = createRoute(routeId, "code", "name");

        LocalDate specialDay = LocalDate.of(2020, 11, 10);

        // does not run on special day
        MutableService serviceA = new MutableService(serviceIdA);
        MutableServiceCalendar calendarA = new MutableServiceCalendar(DateRange.of(startDateA, endDateA), ALL_DAYS);
        calendarA.excludeDate(specialDay);
        serviceA.setCalendar(calendarA);

        // runs ONLY on special day
        MutableService serviceB = new MutableService(serviceIdB);
        MutableServiceCalendar calendarB = new MutableServiceCalendar(DateRange.of(startDateA, endDateA), NO_DAYS);
        calendarB.includeExtraDate(specialDay);
        serviceB.setCalendar(calendarB);

        route.addService(serviceA);
        route.addService(serviceB);

        assertTrue(route.isAvailableOn(specialDay));

    }

    @Test
    void shouldRespectAdditionalDaysOnService() {
        LocalDate startDate = LocalDate.of(2020, 11, 5);
        LocalDate endDate = LocalDate.of(2020, 11, 25);

        MutableRoute route = createRoute(routeId, "code", "name");

        MutableService service = new MutableService(serviceId);
        MutableServiceCalendar calendar = new MutableServiceCalendar(DateRange.of(startDate, endDate), NO_DAYS);
        LocalDate extraRunningDate = LocalDate.of(2020, 11, 10);
        calendar.includeExtraDate(extraRunningDate);
        service.setCalendar(calendar);

        route.addService(service);
        route.addTrip(new MutableTrip(StringIdFor.createId("tripA"), "headSignA", service, route, Tram));

        assertTrue(route.isAvailableOn(extraRunningDate));
    }

    @Test
    void shouldRespectNotRunningDaysOnService() {
        LocalDate startDate = LocalDate.of(2020, 11, 5);
        LocalDate endDate = LocalDate.of(2020, 11, 25);

        MutableRoute route = createRoute(routeId, "code", "name");

        LocalDate notRunningDate = LocalDate.of(2020, 11, 10);

        MutableService service = new MutableService(serviceId);
        MutableServiceCalendar calendar = new MutableServiceCalendar(DateRange.of(startDate, endDate), ALL_DAYS);
        calendar.excludeDate(notRunningDate);
        service.setCalendar(calendar);

        route.addService(service);

        assertFalse(route.isAvailableOn(notRunningDate));
    }

    @Test
    void shouldHaveDateOverlap() {

        LocalDate startDate = LocalDate.of(2020, 11, 5);
        LocalDate endDate = LocalDate.of(2020, 11, 25);

        EnumSet<DayOfWeek> monday = EnumSet.of(MONDAY);

        Service serviceA = createService(startDate, endDate, "serviceA", monday);
        Service serviceB = createService(startDate, endDate, "serviceB", EnumSet.of(DayOfWeek.SUNDAY));
        Service serviceC = createService(startDate.minusDays(10), startDate.minusDays(5), "serviceC", monday);
        Service serviceD = createService(startDate, endDate, "serviceD", monday);

        MutableRoute routeA = createRoute(routeIdA, "codeA", "nameA");
        routeA.addService(serviceA);

        assertTrue(routeA.isDateOverlap(routeA));

        MutableRoute routeB = createRoute(routeIdB, "codeB1", "nameB1");
        routeB.addService(serviceB);

        // wrong operating days
        assertFalse(routeA.isDateOverlap(routeB));

        MutableRoute routeC = createRoute(StringIdFor.createId("routeIdC"), "codeC", "nameC");
        routeC.addService(serviceC);

        // before dates
        assertFalse(routeA.isDateOverlap(routeC));

        MutableRoute routeD = createRoute(StringIdFor.createId("routeIdD"), "codeD", "nameD");
        routeD.addService(serviceD);

        // should match
        assertTrue(routeA.isDateOverlap(routeD));
    }

    private MutableService createService(LocalDate startDate, LocalDate endDate, String serviceId, EnumSet<DayOfWeek> daysOfWeek) {
        MutableService service = new MutableService(StringIdFor.createId(serviceId));
        MutableServiceCalendar calendar = new MutableServiceCalendar(DateRange.of(startDate, endDate), daysOfWeek);
        service.setCalendar(calendar);
        return service;
    }

}
