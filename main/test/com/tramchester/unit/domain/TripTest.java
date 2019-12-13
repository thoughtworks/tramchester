package com.tramchester.unit.domain;

import com.tramchester.domain.*;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.input.Stop;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.ServiceTime;
import com.tramchester.integration.Stations;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalTime;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;

public class TripTest {

    private Location stationA;
    private Location stationB;
    private Trip trip;
    private Location stationC;
    private String routeId;
    private String serviceId;

    @Before
    public void beforeEachTestRuns() {
        trip = new Trip("tripId","headSign", "svcId", "routeId");
        stationA = new Station("statA","areaA", "stopNameA", new LatLong(1.0, -1.0), false);
        stationB = new Station("statB","areaA", "stopNameB", new LatLong(2.0, -2.0), false);
        stationC = new Station("statC","areaA", "stopNameB", new LatLong(2.0, -2.0), false);
        routeId = "routeId";
        serviceId = "serviceId";
    }

    @Test
    public void shouldModelCircularTripsCorrectly() throws TramchesterException {

        Stop firstStop = new Stop("statA1", stationA, 1, TramTime.create(10, 00), TramTime.create(10, 01));
        Stop secondStop = new Stop("statB1", stationB, 2, TramTime.create(10, 05), TramTime.create(10, 06));
        Stop thirdStop = new Stop("statA1", stationA, 3, TramTime.create(10, 10), TramTime.create(10, 10));

        trip.addStop(firstStop);
        trip.addStop(secondStop);
        trip.addStop(thirdStop);

        TramTime am10Minutes = TramTime.of(10,00);
        SortedSet<ServiceTime> times = trip.getServiceTimes("statA", "statB", new TimeWindow(am10Minutes, 30));

        // service times
        assertEquals(1, times.size());
        ServiceTime time = times.first();
        assertEquals(TramTime.create(10, 01), time.getDepartureTime());
        assertEquals(TramTime.create(10, 05), time.getArrivalTime());
        assertEquals("svcId", time.getServiceId());
        assertEquals(am10Minutes.plusMinutes(1), time.getLeaves());

        // services times
        times = trip.getServiceTimes("statB", "statA", new TimeWindow(am10Minutes, 30));
        assertEquals(1, times.size());
        time = times.first();
        assertEquals(TramTime.create(10, 06), time.getDepartureTime());
        assertEquals(TramTime.create(10, 10), time.getArrivalTime());
        assertEquals("svcId", time.getServiceId());
        assertEquals(am10Minutes.plusMinutes(6), time.getLeaves());

        // earliest departs
        assertEquals(am10Minutes.plusMinutes(1), trip.earliestDepartFor("statA","statB", new TimeWindow(am10Minutes, 30)).get().getLeaves());
        assertEquals(am10Minutes.plusMinutes(6), trip.earliestDepartFor("statB","statA", new TimeWindow(am10Minutes, 30)).get().getLeaves());
        assertEquals(am10Minutes.plusMinutes(1), trip.earliestDepartFor("statA","statA", new TimeWindow(am10Minutes, 30)).get().getLeaves());

        assertEquals(TramTime.create(10,01), trip.earliestDepartTime());

        // sequence respected
        List<Integer> seqNums = new LinkedList<>();
        trip.getStops().forEach(stop -> { seqNums.add(stop.getGetSequenceNumber()); });
        assertEquals(1, seqNums.get(0).intValue());
        assertEquals(2, seqNums.get(1).intValue());
        assertEquals(3, seqNums.get(2).intValue());
    }

    @Test
    public void shouldFindEarliestDepartCorrectlyCrossingMidnight() throws TramchesterException {

        Stop firstStop = new Stop("stop1", stationA, 2, TramTime.create(23, 45), TramTime.create(23, 46));
        Stop secondStop = new Stop("stop2", stationB, 3, TramTime.create(23, 59), TramTime.create(0, 1));
        Stop thirdStop = new Stop("stop3", stationC, 4, TramTime.create(0,10), TramTime.create(00, 11));
        Stop fourthStop = new Stop("stop4", stationC, 1, TramTime.create(6,30), TramTime.create(6, 30));

        trip.addStop(firstStop);
        trip.addStop(secondStop);
        trip.addStop(thirdStop);
        trip.addStop(fourthStop);

        SortedSet<ServiceTime> times = trip.getServiceTimes("statA", "statB", new TimeWindow(TramTime.of(23,40), 30));
        assertEquals(1, times.size());
        assertEquals(TramTime.create(23,46), times.first().getDepartureTime());

        times = trip.getServiceTimes("statA", "statB", new TimeWindow(TramTime.of(00,10), 30));
        assertEquals(0, times.size());

        times = trip.getServiceTimes("statB", "statC", new TimeWindow(TramTime.of(23,59), 30));
        assertEquals(2, times.size());

        assertEquals(TramTime.create(6,30), trip.earliestDepartTime());
    }

    @Test
    public void shouldFindEarliestDepartCorrectly() throws TramchesterException {

        Stop thirdStop = new Stop("stop3", stationC, 3, TramTime.create(0,10), TramTime.create(00, 11));
        Stop fourthStop = new Stop("stop4", stationC, 1, TramTime.create(6,30), TramTime.create(6, 31));

        trip.addStop(thirdStop);
        trip.addStop(fourthStop);

        assertEquals(TramTime.create(6,31), trip.earliestDepartTime());
    }

    @Test
    public void shouldFindLatestDepartCorrectly() {
        trip.addStop(new Stop("stopId3", Stations.Deansgate, 3, TramTime.of(10,25), TramTime.of(10,26)));
        trip.addStop(new Stop("stopId4", Stations.Deansgate, 4, TramTime.of(0,1), TramTime.of(0,1)));

        assertEquals(TramTime.of(0,1), trip.latestDepartTime());

    }

    @Test
    public void shouldFindEarliestDepartAfterCrossingMidnight() throws TramchesterException {

        Stop firstStop = new Stop("stop1", stationA, 1, TramTime.create(00, 10), TramTime.create(00, 11));
        Stop secondStop = new Stop("stop2", stationB, 2, TramTime.create(00, 15), TramTime.create(0, 16));
        Stop thirdStop = new Stop("stop3", stationC, 3, TramTime.create(0,24), TramTime.create(00, 25));

        trip.addStop(firstStop);
        trip.addStop(secondStop);
        trip.addStop(thirdStop);

        SortedSet<ServiceTime> times = trip.getServiceTimes("statA", "statB", new TimeWindow(TramTime.of(23,40), 37));
        assertEquals(1, times.size());
        assertEquals(TramTime.create(00,11), times.first().getDepartureTime());

        times = trip.getServiceTimes("statA", "statB", new TimeWindow(TramTime.of(00,10), 30));
        assertEquals(1, times.size());
        assertEquals(TramTime.create(00,11), times.first().getDepartureTime());

        assertEquals(TramTime.create(0,11), trip.earliestDepartTime());

    }
}
