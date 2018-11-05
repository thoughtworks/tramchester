package com.tramchester.unit.domain;

import com.tramchester.domain.DaysOfWeek;
import com.tramchester.domain.Service;
import com.tramchester.domain.input.Trip;
import org.junit.Test;

import java.time.LocalDate;
import java.util.HashMap;

import static junit.framework.TestCase.assertFalse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

public class ServiceTest {

    @Test
    public void shouldSetStartDateAndEndDate() throws Exception {

        Service service = new Service("", "");

        LocalDate startDate = LocalDate.of(2014, 10, 5);
        LocalDate endDate = LocalDate.of(2014, 12, 25);

        service.setServiceDateRange(startDate, endDate);

        assertThat(service.getStartDate().getDate()).isEqualTo(startDate);
        assertThat(service.getEndDate().getDate()).isEqualTo(endDate);

        assertTrue(service.operatesOn(startDate));
        assertTrue(service.operatesOn(endDate));
        assertTrue(service.operatesOn(LocalDate.of(2014,11,30)));

        assertFalse(service.operatesOn(LocalDate.of(2016,11,30)));
        assertFalse(service.operatesOn(startDate.minusDays(1)));
        assertFalse(service.operatesOn(endDate.plusDays(1)));
    }

    @Test
    public void shouldAddTripsToService() throws Exception {

        Service service = new Service("", "");

        Trip trip = new Trip("001", "Deansgate", "SVC002", "");
        service.addTrip(trip);

        assertThat(service.getTrips()).hasSize(1);
        assertThat(service.getTrips()).contains(trip);
    }

    @Test
    public void shouldSetWeekendDaysOnService() throws Exception {
        Service service = new Service("", "");

        service.setDays(false, false, false, false, false, true, true);

        HashMap<DaysOfWeek, Boolean> days = service.getDays();
        assertThat(days.get(DaysOfWeek.Monday)).isFalse();
        assertThat(days.get(DaysOfWeek.Tuesday)).isFalse();
        assertThat(days.get(DaysOfWeek.Wednesday)).isFalse();
        assertThat(days.get(DaysOfWeek.Thursday)).isFalse();
        assertThat(days.get(DaysOfWeek.Friday)).isFalse();
        assertThat(days.get(DaysOfWeek.Saturday)).isTrue();
        assertThat(days.get(DaysOfWeek.Sunday)).isTrue();
    }

    @Test
    public void shouldSetRouteIdAndServiceId() throws Exception {
        Service service = new Service("SRV001", "ROUTE66");

        assertThat(service.getRouteId()).isEqualTo("ROUTE66");
        assertThat(service.getServiceId()).isEqualTo("SRV001");
    }


    @Test
    public void shouldCheckIfServiceIsNotRunning() throws Exception {
        Service service = new Service("", "");

        service.setDays(false, false, false, false, false, false, false);

        assertThat(service.isRunning()).isFalse();
    }

    @Test
    public void shouldCheckIfServiceIsRunning() throws Exception {
        Service service = new Service("", "");

        service.setDays(false, false, true, false, false, false, false);

        assertThat(service.isRunning()).isTrue();
    }


}