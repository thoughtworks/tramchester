package com.tramchester.unit.domain;

import com.tramchester.dataimport.data.CalendarDateData;
import com.tramchester.domain.Service;
import com.tramchester.domain.input.TramStopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.time.TramTime;
import com.tramchester.testSupport.Stations;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static com.tramchester.domain.Platform.from;
import static org.assertj.core.api.Assertions.assertThat;

class ServiceTest {

    @Test
    void shouldSetStartDateAndEndDate() {

        Service service = new Service("", TestEnv.getTestRoute());

        LocalDate startDate = LocalDate.of(2014, 10, 5);
        LocalDate endDate = LocalDate.of(2014, 12, 25);

        service.setDays(true,true,true,true,true,true, true);

        service.setServiceDateRange(startDate, endDate);

        Assertions.assertTrue(service.operatesOn(startDate));
        Assertions.assertTrue(service.operatesOn(endDate));
        Assertions.assertTrue(service.operatesOn(LocalDate.of(2014,11,30)));

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
        Assertions.assertTrue(service.operatesOn(queryDate));
        service.addExceptionDate(queryDate, CalendarDateData.REMOVED);
        Assertions.assertFalse(service.operatesOn(queryDate));
    }

    @Test
    void shouldCheckIfServiceHasExceptionDatesAdded() {
        Service service = new Service("", TestEnv.getTestRoute());

        LocalDate startDate = TestEnv.LocalNow().toLocalDate();
        LocalDate endDate = TestEnv.nextTuesday(14);
        service.setServiceDateRange(startDate, endDate);

        service.setDays(false, false, false, false, false, false, true);
        Assertions.assertTrue(service.operatesOn(TestEnv.nextSunday()));

        LocalDate weekTuesday = TestEnv.nextTuesday(7);
        Assertions.assertFalse(service.operatesOn(weekTuesday));
        service.addExceptionDate(weekTuesday, CalendarDateData.ADDED);
        Assertions.assertTrue(service.operatesOn(weekTuesday));
    }

    @Test
    void shouldAddTripsToService() {

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

        Assertions.assertEquals(TramTime.of(6,30), service.earliestDepartTime());
        Assertions.assertEquals(TramTime.of(0,1), service.latestDepartTime());
    }

    @Test
    void shouldSetWeekendDaysOnService() {
        Service service = new Service("", TestEnv.getTestRoute());

        service.setServiceDateRange(TestEnv.LocalNow().toLocalDate(), TestEnv.nextTuesday(14));
        service.setDays(false, false, false, false, false, true, true);

        Assertions.assertFalse(service.operatesOn(TestEnv.nextTuesday(7)));
        Assertions.assertTrue(service.operatesOn(TestEnv.nextSaturday()));
        Assertions.assertTrue(service.operatesOn(TestEnv.nextSunday()));
    }

    @Test
    void shouldSetRouteIdAndServiceId() {
        Service service = new Service("SRV001", TestEnv.getTestRoute("ROUTE66"));

        assertThat(service.getRouteId()).isEqualTo("ROUTE66");
        assertThat(service.getId()).isEqualTo("SRV001");
    }

    @Test
    void shouldNoticeNoDatesSet() {
        Service service = new Service("svcXXX", TestEnv.getTestRoute("ROUTE66"));
        Assertions.assertTrue(service.HasMissingDates());

        service.setServiceDateRange(LocalDate.MIN, LocalDate.MAX);
        Assertions.assertTrue(service.HasMissingDates());

        service.setServiceDateRange(TestEnv.LocalNow().toLocalDate(), TestEnv.nextTuesday(0));
        Assertions.assertTrue(service.HasMissingDates());// no days set

        service.setDays(true, false, false, false, false, false, false);
        Assertions.assertFalse(service.HasMissingDates()); // now have days

        service.setServiceDateRange(LocalDate.MIN, LocalDate.MAX);
        Assertions.assertTrue(service.HasMissingDates()); // invalid dates

    }

    @Test
    void shouldReportNoDatesSetIncludingExceptions() {
        Service service = new Service("svcXXX", TestEnv.getTestRoute("ROUTE66"));

        service.setDays(true, false, false, false, false, false, false);

        Assertions.assertTrue(service.HasMissingDates()); // missing dates

        service.addExceptionDate(TestEnv.nextTuesday(0), CalendarDateData.ADDED);
        Assertions.assertFalse(service.HasMissingDates());
    }

}