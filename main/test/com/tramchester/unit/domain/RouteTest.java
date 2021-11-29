package com.tramchester.unit.domain;

import com.tramchester.domain.*;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.DateRange;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

import static com.tramchester.domain.id.StringIdFor.createId;
import static java.time.DayOfWeek.MONDAY;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteTest {

    @Test
    void shouldHaveTramRoute() {
        Route route = MutableRoute.getRoute(createId("idA"),"code","name", TestEnv.MetAgency(),
                TransportMode.Tram);
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

        MutableRoute route = new MutableRoute(createId("routeId"),"code","name", TestEnv.MetAgency(),
                TransportMode.Tram);

        route.addService(createService(startDate, endDate,"serviceId", EnumSet.of(MONDAY)));
        route.addService(createService(startDate, endDate,"serviceId", EnumSet.of(MONDAY)));
        route.addService(createService(startDate, endDate,"serviceId2", EnumSet.of(MONDAY)));

        Set<Service> services = route.getServices();

        Assertions.assertEquals(2, services.size());
    }

    @Test
    void shouldHaveDateOverlop() {
        MutableRoute routeA = new MutableRoute(createId("routeIdA"),"codeA","nameA", TestEnv.MetAgency(),
                TransportMode.Tram);
        MutableRoute routeB = new MutableRoute(createId("routeIdB"),"codeB","nameB", TestEnv.MetAgency(),
                TransportMode.Tram);

        LocalDate startDate = LocalDate.of(2020, 11, 5);
        LocalDate endDate = LocalDate.of(2020, 11, 25);

        Service serviceA = createService(startDate, endDate, "serviceA", EnumSet.of(MONDAY));
        routeA.addService(serviceA);
        Service serviceB = createService(startDate, endDate, "serviceB", EnumSet.of(DayOfWeek.SUNDAY));

        routeB = new MutableRoute(createId("routeIdB"),"codeB","nameB", TestEnv.MetAgency(),
                TransportMode.Tram);
        routeB.addService(serviceB);
        assertFalse(routeA.isDateOverlap(routeB));

        routeB = new MutableRoute(createId("routeIdB"),"codeB","nameB", TestEnv.MetAgency(),
                TransportMode.Tram);
        Service serviceC = createService(startDate.minusDays(10), startDate.minusDays(5), "serviceC", EnumSet.of(MONDAY));
        routeB.addService(serviceC);
        assertFalse(routeA.isDateOverlap(routeB));

        routeB = new MutableRoute(createId("routeIdB"),"codeB","nameB", TestEnv.MetAgency(),
                TransportMode.Tram);
        Service serviceD = createService(startDate, endDate, "serviceD", EnumSet.of(MONDAY));
        routeB.addService(serviceD);
        assertTrue(routeA.isDateOverlap(routeB));
    }

    private MutableService createService(LocalDate startDate, LocalDate endDate, String serviceId, EnumSet<DayOfWeek> daysOfWeek) {
        MutableService service = new MutableService(StringIdFor.createId(serviceId));
        MutableServiceCalendar calendar = new MutableServiceCalendar(DateRange.of(startDate, endDate), daysOfWeek);
        service.setCalendar(calendar);
        return service;
    }

}
