package com.tramchester.unit.domain;

import com.tramchester.domain.*;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.integration.Stations;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalTime;

import static org.junit.Assert.assertEquals;

public class WalkingStageTest {

    private WalkingStage walkingStage;
    private Location start;
    private Location destination;

    @Before
    public void setUp() {
        LocalTime begin = LocalTime.of(8,0); //8 * 60;
        int cost = 22;
        start = Stations.Altrincham;
        destination = Stations.Cornbrook;
        walkingStage = new WalkingStage(new RawWalkingStage(start, destination, cost), begin);
    }

    @Test
    public void shouldCalculateTimesCorrectly() throws TramchesterException {

        TramTime arrival = walkingStage.getExpectedArrivalTime();
        assertEquals(TramTime.create(8,22), arrival);

        TramTime departTime = walkingStage.getFirstDepartureTime();
        assertEquals(TramTime.create(8,00), departTime);

        assertEquals(22, walkingStage.getDuration());
    }

    @Test
    public void shouldReportModeCorrectly() {
        assertEquals(TransportMode.Walk, walkingStage.getMode());
    }

    @Test
    public void shouldGetStartAndDestinaiton() {
        assertEquals(start, walkingStage.getFirstStation()); // comment in the class
        assertEquals(destination, walkingStage.getLastStation());
        assertEquals(destination, walkingStage.getActionStation());
    }

}
