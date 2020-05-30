package com.tramchester.unit.domain;

import com.tramchester.dataimport.data.CalendarDateData;
import com.tramchester.domain.time.DaysOfWeek;
import com.tramchester.domain.Service;
import com.tramchester.domain.time.TramTime;
import com.tramchester.domain.input.TramStopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.testSupport.Stations;
import com.tramchester.testSupport.TestEnv;
import org.junit.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;

import static com.tramchester.domain.Platform.from;
import static junit.framework.TestCase.assertFalse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

public class ServiceTest {

    @Test
    public void shouldSetStartDateAndEndDate() {

        Service service = new Service("", TestEnv.getTestRoute());

        LocalDate startDate = LocalDate.of(2014, 10, 5);
        LocalDate endDate = LocalDate.of(2014, 12, 25);

        service.setDays(true,true,true,true,true,true, true);

        service.setServiceDateRange(startDate, endDate);

        assertTrue(service.operatesOn(startDate));
        assertTrue(service.operatesOn(endDate));
        assertTrue(service.operatesOn(LocalDate.of(2014,11,30)));

        assertFalse(service.operatesOn(LocalDate.of(2016,11,30)));
        assertFalse(service.operatesOn(startDate.minusDays(1)));
        assertFalse(service.operatesOn(endDate.plusDays(1)));
    }

    @Test
    public void shouldCheckIfServiceHasExceptionDatesRemoved() {
        Service service = new Service("", TestEnv.getTestRoute());
        LocalDate startDate = LocalDate.of(2020, 10, 5);
        LocalDate endDate = LocalDate.of(2020, 12, 10);
        service.setServiceDateRange(startDate, endDate);
        service.setDays(true,true,true,true,true,true,true);

        LocalDate queryDate = LocalDate.of(2020, 12, 1);
        assertTrue(service.operatesOn(queryDate));
        service.addExceptionDate(queryDate, CalendarDateData.REMOVED);
        assertFalse(service.operatesOn(queryDate));
    }

    @Test
    public void shouldCheckIfServiceHasExceptionDatesAdded() {
        Service service = new Service("", TestEnv.getTestRoute());

        LocalDate startDate = TestEnv.LocalNow().toLocalDate();
        LocalDate endDate = TestEnv.nextTuesday(14);
        service.setServiceDateRange(startDate, endDate);

        service.setDays(false, false, false, false, false, false, true);
        assertTrue(service.operatesOn(TestEnv.nextSunday()));

        LocalDate weekTuesday = TestEnv.nextTuesday(7);
        assertFalse(service.operatesOn(weekTuesday));
        service.addExceptionDate(weekTuesday, CalendarDateData.ADDED);
        assertTrue(service.operatesOn(weekTuesday));
    }

    @Test
    public void shouldAddTripsToService() {

        Service service = new Service("SVC002", TestEnv.getTestRoute());
        Trip trip = new Trip("001", "Deansgate", service, TestEnv.getTestRoute());
        trip.addStop(new TramStopCall(from("stopId1"), Stations.Deansgate, (byte) 3, TramTime.of(9,5), TramTime.of(9,6)));
        trip.addStop(new TramStopCall(from("stopId2"), Stations.Deansgate, (byte) 2, TramTime.of(8,15), TramTime.of(8,16)));
        trip.addStop(new TramStopCall(from("stopId3"), Stations.Deansgate, (byte) 4, TramTime.of(10,25), TramTime.of(10,26)));
        trip.addStop(new TramStopCall(from("stopId4"), Stations.Deansgate, (byte) 5, TramTime.of(0,1), TramTime.of(0,1)));
        trip.addStop(new TramStopCall(from("stopId5"), Stations.Deansgate, (byte) 1, TramTime.of(6,30), TramTime.of(6,30)));

        service.addTrip(trip);

        assertThat(service.getTrips()).hasSize(1);
        assertThat(service.getTrips()).contains(trip);

        assertEquals(TramTime.of(6,30), service.earliestDepartTime());
        assertEquals(TramTime.of(0,1), service.latestDepartTime());
    }

    @Test
    public void shouldSetWeekendDaysOnService() {
        Service service = new Service("", TestEnv.getTestRoute());

        service.setServiceDateRange(TestEnv.LocalNow().toLocalDate(), TestEnv.nextTuesday(14));
        service.setDays(false, false, false, false, false, true, true);

        assertFalse(service.operatesOn(TestEnv.nextTuesday(7)));
        assertTrue(service.operatesOn(TestEnv.nextSaturday()));
        assertTrue(service.operatesOn(TestEnv.nextSunday()));
    }

    @Test
    public void shouldSetRouteIdAndServiceId() {
        Service service = new Service("SRV001", TestEnv.getTestRoute("ROUTE66"));

        assertThat(service.getRouteId()).isEqualTo("ROUTE66");
        assertThat(service.getId()).isEqualTo("SRV001");
    }

    @Test
    public void shouldNoticeNoDatesSet() {
        Service service = new Service("svcXXX", TestEnv.getTestRoute("ROUTE66"));
        assertTrue(service.HasMissingDates());

        service.setServiceDateRange(LocalDate.MIN, LocalDate.MAX);
        assertTrue(service.HasMissingDates());

        service.setServiceDateRange(TestEnv.LocalNow().toLocalDate(), TestEnv.nextTuesday(0));
        assertTrue(service.HasMissingDates());// no days set

        service.setDays(true, false, false, false, false, false, false);
        assertFalse(service.HasMissingDates()); // now have days

        service.setServiceDateRange(LocalDate.MIN, LocalDate.MAX);
        assertTrue(service.HasMissingDates()); // invalid dates

    }

    @Test
    public void shouldReportNoDatesSetIncludingExceptions() {
        Service service = new Service("svcXXX", TestEnv.getTestRoute("ROUTE66"));

        service.setDays(true, false, false, false, false, false, false);

        assertTrue(service.HasMissingDates()); // missing dates

        service.addExceptionDate(TestEnv.nextTuesday(0), CalendarDateData.ADDED);
        assertFalse(service.HasMissingDates());
    }

}