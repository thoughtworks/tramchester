package com.tramchester.unit.domain;

import com.tramchester.domain.*;
import com.tramchester.domain.input.Stop;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.Stations;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;

public class TripTest {

    private Station stationA;
    private Station stationB;
    private Trip trip;
    private Station stationC;

    @Before
    public void beforeEachTestRuns() {
        trip = new Trip("tripId","headSign", "svcId", "routeId");
        stationA = new Station("statA","areaA", "stopNameA", new LatLong(1.0, -1.0), false);
        stationB = new Station("statB","areaA", "stopNameB", new LatLong(2.0, -2.0), false);
        stationC = new Station("statC","areaA", "stopNameB", new LatLong(2.0, -2.0), false);
    }

    @Test
    public void shouldModelCircularTripsCorrectly() {

        Stop firstStop = new Stop("statA1", stationA, 1, TramTime.of(10, 00), TramTime.of(10, 01));
        Stop secondStop = new Stop("statB1", stationB, 2, TramTime.of(10, 05), TramTime.of(10, 06));
        Stop thirdStop = new Stop("statA1", stationA, 3, TramTime.of(10, 10), TramTime.of(10, 10));

        trip.addStop(firstStop);
        trip.addStop(secondStop);
        trip.addStop(thirdStop);

        assertEquals(TramTime.of(10,01), trip.earliestDepartTime());

        // sequence respected
        List<Integer> seqNums = new LinkedList<>();
        trip.getStops().forEach(stop -> { seqNums.add(stop.getGetSequenceNumber()); });
        assertEquals(1, seqNums.get(0).intValue());
        assertEquals(2, seqNums.get(1).intValue());
        assertEquals(3, seqNums.get(2).intValue());
    }

    @Test
    public void shouldFindEarliestDepartCorrectlyCrossingMidnight() {

        Stop firstStop = new Stop("stop1", stationA, 2, TramTime.of(23, 45), TramTime.of(23, 46));
        Stop secondStop = new Stop("stop2", stationB, 3, TramTime.of(23, 59), TramTime.of(0, 1));
        Stop thirdStop = new Stop("stop3", stationC, 4, TramTime.of(0,10), TramTime.of(00, 11));
        Stop fourthStop = new Stop("stop4", stationC, 1, TramTime.of(6,30), TramTime.of(6, 30));

        trip.addStop(firstStop);
        trip.addStop(secondStop);
        trip.addStop(thirdStop);
        trip.addStop(fourthStop);

        assertEquals(TramTime.of(6,30), trip.earliestDepartTime());
    }

    @Test
    public void shouldFindEarliestDepartCorrectly() {

        Stop thirdStop = new Stop("stop3", stationC, 3, TramTime.of(0,10), TramTime.of(00, 11));
        Stop fourthStop = new Stop("stop4", stationC, 1, TramTime.of(6,30), TramTime.of(6, 31));

        trip.addStop(thirdStop);
        trip.addStop(fourthStop);

        assertEquals(TramTime.of(6,31), trip.earliestDepartTime());
    }

    @Test
    public void shouldFindLatestDepartCorrectly() {
        trip.addStop(new Stop("stopId3", Stations.Deansgate, 3, TramTime.of(10,25), TramTime.of(10,26)));
        trip.addStop(new Stop("stopId4", Stations.Deansgate, 4, TramTime.of(0,1), TramTime.of(0,1)));

        assertEquals(TramTime.of(0,1), trip.latestDepartTime());

    }

}
