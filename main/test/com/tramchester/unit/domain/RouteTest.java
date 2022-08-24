package com.tramchester.unit.domain;

import com.tramchester.domain.*;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.MutableServiceCalendar;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.input.MutableTrip;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.testSupport.TestEnv;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
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
    private IdFor<Route> routeId;
    private IdFor<Service> serviceId;

    @BeforeEach
    void setUp() {
        NO_DAYS = EnumSet.noneOf(DayOfWeek.class);
        ALL_DAYS = EnumSet.allOf(DayOfWeek.class);
        routeId = createId("routeId");
        serviceId = StringIdFor.createId("serviceId");
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
        TramDate startDate = TramDate.of(2020, 11, 5);
        TramDate endDate = TramDate.of(2020, 11, 25);

        MutableRoute route = createRoute(routeId, "code", "name");

        route.addService(createService(startDate, endDate,"serviceId", EnumSet.of(MONDAY)));
        route.addService(createService(startDate, endDate,"serviceId", EnumSet.of(MONDAY)));
        route.addService(createService(startDate, endDate,"serviceId2", EnumSet.of(MONDAY)));

        Set<Service> services = route.getServices();

        Assertions.assertEquals(2, services.size());
    }

    @Test
    void shouldAddTrip() {
        TramDate startDate = TramDate.of(2020, 11, 5);
        TramDate endDate = TramDate.of(2020, 11, 25);

        MutableRoute route = createRoute(routeId, "code", "name");

        final Service serviceA = createService(startDate, endDate, "serviceId", EnumSet.of(MONDAY));

        route.addTrip(new MutableTrip(StringIdFor.createId("tripA"), "headSignA", serviceA, route, Tram));
        route.addTrip(new MutableTrip(StringIdFor.createId("tripB"), "headSignB", serviceA, route, Tram));

        Set<Trip> trips = route.getTrips();

        Assertions.assertEquals(2, trips.size());
    }

    @Test
    void shouldRespectDateRangesOnService() {
        final TramDate startDate = TramDate.of(2020, 11, 5);
        final TramDate endDate = TramDate.of(2020, 11, 25);

        MutableRoute route = createRoute(routeId, "code", "name");

        MutableService service = new MutableService(serviceId);
        MutableServiceCalendar calendar = new MutableServiceCalendar(DateRange.of(startDate, endDate), ALL_DAYS);
        service.setCalendar(calendar);

        route.addService(service);

        assertFalse(route.isAvailableOn(startDate.minusDays(1)));
        assertFalse(route.isAvailableOn(endDate.plusDays(1)));

        TramDate date = startDate;
        while (date.isBefore(endDate)) {
            assertTrue(service.getCalendar().operatesOn(date));
            assertTrue(route.isAvailableOn(date), "should be available on " + date);
            date = date.plusDays(1);
        }

    }

    @NotNull
    private MutableRoute createRoute(IdFor<Route> routeIdA, String code, String nameA) {
        return new MutableRoute(routeIdA, code, nameA, TestEnv.MetAgency(),
                Tram);
    }

    @Test
    void shouldRespectAdditionalDaysOnService() {
        TramDate startDate = TramDate.of(2020, 11, 5);
        TramDate endDate = TramDate.of(2020, 11, 25);

        MutableRoute route = createRoute(routeId, "code", "name");

        MutableService service = new MutableService(serviceId);
        MutableServiceCalendar calendar = new MutableServiceCalendar(DateRange.of(startDate, endDate), NO_DAYS);
        TramDate extraRunningDate = TramDate.of(2020, 11, 10);
        calendar.includeExtraDate(extraRunningDate);
        service.setCalendar(calendar);

        route.addService(service);

        assertTrue(route.isAvailableOn(extraRunningDate));
    }

    @Test
    void shouldRespectNotRunningDaysOnService() {
        TramDate startDate = TramDate.of(2020, 11, 5);
        TramDate endDate = TramDate.of(2020, 11, 25);

        MutableRoute route = createRoute(routeId, "code", "name");

        TramDate notRunningDate = TramDate.of(2020, 11, 10);

        MutableService service = new MutableService(serviceId);
        MutableServiceCalendar calendar = new MutableServiceCalendar(DateRange.of(startDate, endDate), ALL_DAYS);
        calendar.excludeDate(notRunningDate);
        service.setCalendar(calendar);

        route.addService(service);

        assertFalse(route.isAvailableOn(notRunningDate));
    }


    private MutableService createService(TramDate startDate, TramDate endDate, String serviceId, EnumSet<DayOfWeek> daysOfWeek) {
        MutableService service = new MutableService(StringIdFor.createId(serviceId));
        MutableServiceCalendar calendar = new MutableServiceCalendar(DateRange.of(startDate, endDate), daysOfWeek);
        service.setCalendar(calendar);
        return service;
    }

}
