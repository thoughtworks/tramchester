package com.tramchester.unit.domain;

import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.Service;
import com.tramchester.domain.ServiceCalendar;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.time.TramTime;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceTest {

    @Test
    void shouldAddTripsToService() {

        Service service = new Service("SVC002", TestEnv.getTestRoute());
        Trip trip = new Trip("001", "Deansgate", service, TestEnv.getTestRoute());
        StringIdFor<Trip> tripId = trip.getId();
        TramStations deansgate = TramStations.Deansgate;
        trip.addStop(TestEnv.createTramStopCall(tripId, "stopId1", deansgate,3, TramTime.of(9, 5), TramTime.of(9, 6)));
        trip.addStop(TestEnv.createTramStopCall(tripId, "stopId2", deansgate, 2, TramTime.of(8, 15), TramTime.of(8, 16)));
        trip.addStop(TestEnv.createTramStopCall(tripId, "stopId3", deansgate, 4, TramTime.of(10, 25), TramTime.of(10, 26)));
        trip.addStop(TestEnv.createTramStopCall(tripId, "stopId4", deansgate,  5, TramTime.of(0, 1), TramTime.of(0, 1)));
        trip.addStop(TestEnv.createTramStopCall(tripId, "stopId5", deansgate, 1, TramTime.of(6, 30), TramTime.of(6, 30)));

        service.addTrip(trip);

        Set<Trip> serviceTrips = service.getTrips();
        assertThat(serviceTrips).hasSize(1);
        assertThat(serviceTrips).contains(trip);

        service.updateTimings();

        Assertions.assertEquals(TramTime.of(6, 30), service.earliestDepartTime());
        Assertions.assertEquals(TramTime.of(0, 1), service.latestDepartTime());
    }

    @Test
    void shouldNoticeNoDatesSet() {
        Service service = new Service("svcXXX", TestEnv.getTestRoute(StringIdFor.createId("ROUTE66")));
        assertFalse(service.hasCalendar());

        LocalDate startDate = LocalDate.of(2014, 10, 5);
        LocalDate endDate = LocalDate.of(2014, 12, 25);

        ServiceCalendar serviceCalendar = new ServiceCalendar(startDate, endDate, TestEnv.allDays());

        service.setCalendar(serviceCalendar);

        assertTrue(service.hasCalendar());
    }

}