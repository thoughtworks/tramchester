package com.tramchester.domain;


import com.tramchester.Stations;
import com.tramchester.domain.exceptions.TramchesterException;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalTime;

import static org.junit.Assert.assertEquals;

public class WalkingStageTest {

    private int begin;
    private int cost;
    private WalkingStage walkingStage;
    private Location start;
    private Location destination;

    @Before
    public void setUp() throws Exception {
        begin = 8*60;
        cost = 22;
        start = Stations.Altrincham;
        destination = Stations.Cornbrook;
        walkingStage = new WalkingStage(start, destination, begin, cost);
    }

    @Test
    public void shouldCalculateTimesCorrectly() {

        LocalTime arrival = walkingStage.getExpectedArrivalTime();
        assertEquals(LocalTime.of(8,22), arrival);

        LocalTime departTime = walkingStage.getFirstDepartureTime();
        assertEquals(LocalTime.of(8,00), departTime);

        assertEquals(22, walkingStage.getDuration());

        assertEquals(1, walkingStage.getNumberOfServiceTimes());
    }

    @Test
    public void shouldGetSummaryAndPrompt() throws TramchesterException {
        assertEquals("Walking",walkingStage.getSummary());
        assertEquals("Walk to",walkingStage.getPrompt());
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
