package com.tramchester.unit.domain;

import com.tramchester.dataimport.data.CalendarDateData;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.time.ServiceTime;
import com.tramchester.testSupport.Stations;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceTest {

    @Test
    void shouldSetStartDateAndEndDate() {

        Service service = new Service("", TestEnv.getTestRoute());

        LocalDate startDate = LocalDate.of(2014, 10, 5);
        LocalDate endDate = LocalDate.of(2014, 12, 25);

        service.setDays(true,true,true,true,true,true, true);

        service.setServiceDateRange(startDate, endDate);

        assertTrue(service.operatesOn(startDate));
        assertTrue(service.operatesOn(endDate));
        assertTrue(service.operatesOn(LocalDate.of(2014,11,30)));

        Assertions.assertFalse(service.operatesOn(LocalDate.of(2016,11,30)));
        Assertions.assertFalse(service.operatesOn(startDate.minusDays(1)));
        Assertions.assertFalse(service.operatesOn(endDate.plusDays(1)));
    }

    @Test
    void shouldCheckIfServiceHasExceptionDatesRemoved() {
        Service service = new Service("", TestEnv.getTestRoute());
        LocalDate startDate = LocalDate.of(2020, 10, 5);
        LocalDate endDate = LocalDate.of(2020, 12, 10);
        service.setServiceDateRange(startDate, endDate);
        service.setDays(true,true,true,true,true,true,true);

        LocalDate queryDate = LocalDate.of(2020, 12, 1);
        assertTrue(service.operatesOn(queryDate));
        service.addExceptionDate(queryDate, CalendarDateData.REMOVED);
        Assertions.assertFalse(service.operatesOn(queryDate));
    }

    @Test
    void shouldCheckIfServiceHasExceptionDatesAdded() {
        Service service = new Service("", TestEnv.getTestRoute());

        LocalDate startDate = TestEnv.LocalNow().toLocalDate();
        LocalDate endDate = TestEnv.testDay().plusWeeks(2);
        service.setServiceDateRange(startDate, endDate);

        service.setDays(false, false, false, false, false, false, true);
        assertTrue(service.operatesOn(TestEnv.nextSunday()));

        LocalDate weekTuesday = TestEnv.testDay().plusWeeks(1);
        Assertions.assertFalse(service.operatesOn(weekTuesday));
        service.addExceptionDate(weekTuesday, CalendarDateData.ADDED);
        assertTrue(service.operatesOn(weekTuesday));
    }

    @Test
    void shouldAddTripsToService() {

        Service service = new Service("SVC002", TestEnv.getTestRoute());
        Trip trip = new Trip("001", "Deansgate", service, TestEnv.getTestRoute());
        IdFor<Trip> tripId = trip.getId();
        trip.addStop(TestEnv.createTramStopCall(tripId, "stopId1", Stations.Deansgate,3, ServiceTime.of(9,5), ServiceTime.of(9,6)));
        trip.addStop(TestEnv.createTramStopCall(tripId, "stopId2", Stations.Deansgate, 2, ServiceTime.of(8,15), ServiceTime.of(8,16)));
        trip.addStop(TestEnv.createTramStopCall(tripId, "stopId3", Stations.Deansgate, 4, ServiceTime.of(10,25), ServiceTime.of(10,26)));
        trip.addStop(TestEnv.createTramStopCall(tripId, "stopId4", Stations.Deansgate,  5, ServiceTime.of(0,1), ServiceTime.of(0,1)));
        trip.addStop(TestEnv.createTramStopCall(tripId, "stopId5", Stations.Deansgate, 1, ServiceTime.of(6,30), ServiceTime.of(6,30)));

        service.addTrip(trip);

        assertThat(service.getAllTrips()).hasSize(1);
        assertThat(service.getAllTrips()).contains(trip);
        assertThat(service.getTripsFor(TestEnv.getTestRoute())).hasSize(1);
        assertThat(service.getTripsFor(TestEnv.getTestRoute()).contains(trip));

        service.updateTimings();

        Assertions.assertEquals(ServiceTime.of(6,30), service.earliestDepartTime());
        Assertions.assertEquals(ServiceTime.of(0,1), service.latestDepartTime());
    }

    @Test
    void shouldSetWeekendDaysOnService() {
        Service service = new Service("", TestEnv.getTestRoute());

        service.setServiceDateRange(TestEnv.LocalNow().toLocalDate(), TestEnv.testDay().plusWeeks(2));
        service.setDays(false, false, false, false, false, true, true);

        Assertions.assertFalse(service.operatesOn(TestEnv.testDay().plusWeeks(1)));
        assertTrue(service.operatesOn(TestEnv.nextSaturday()));
        assertTrue(service.operatesOn(TestEnv.nextSunday()));
    }

    @Test
    void shouldSetRouteIdAndServiceId() {
        Route route66 = TestEnv.getTestRoute("ROUTE66");
        Service service = new Service("SRV001", route66);

        assertThat(service.getRoutes().size()).isEqualTo(1);
        assertTrue(service.getRoutes().contains(route66));
        assertThat(service.getId()).isEqualTo(IdFor.createId("SRV001"));

        Route another = TestEnv.getTestRoute("another");
        service.addRoute(another);

        assertThat(service.getRoutes()).hasSize(2);
    }

    @Test
    void shouldNoticeNoDatesSet() {
        Service service = new Service("svcXXX", TestEnv.getTestRoute("ROUTE66"));
        assertTrue(service.HasMissingDates());

        service.setServiceDateRange(LocalDate.MIN, LocalDate.MAX);
        assertTrue(service.HasMissingDates());

        service.setServiceDateRange(TestEnv.LocalNow().toLocalDate(), TestEnv.testDay());
        assertTrue(service.HasMissingDates());// no days set

        service.setDays(true, false, false, false, false, false, false);
        Assertions.assertFalse(service.HasMissingDates()); // now have days

        service.setServiceDateRange(LocalDate.MIN, LocalDate.MAX);
        assertTrue(service.HasMissingDates()); // invalid dates

    }

    @Test
    void shouldReportNoDatesSetIncludingExceptions() {
        Service service = new Service("svcXXX", TestEnv.getTestRoute("ROUTE66"));

        service.setDays(true, false, false, false, false, false, false);

        assertTrue(service.HasMissingDates()); // missing dates

        service.addExceptionDate(TestEnv.testDay(), CalendarDateData.ADDED);
        Assertions.assertFalse(service.HasMissingDates());
    }

}