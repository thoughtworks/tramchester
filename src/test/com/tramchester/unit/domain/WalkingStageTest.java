package com.tramchester.domain;


import com.tramchester.integration.Stations;
import com.tramchester.domain.exceptions.TramchesterException;
import org.joda.time.LocalTime;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WalkingStageTest {

    private WalkingStage walkingStage;
    private Location start;
    private Location destination;

    @Before
    public void setUp() throws Exception {
        int begin = 8 * 60;
        int cost = 22;
        start = Stations.Altrincham;
        destination = Stations.Cornbrook;
        walkingStage = new WalkingStage(new RawWalkingStage(start, destination, cost), begin);
    }

    @Test
    public void shouldCalculateTimesCorrectly() {

        LocalTime arrival = walkingStage.getExpectedArrivalTime();
        assertEquals(new LocalTime(8,22), arrival);

        LocalTime departTime = walkingStage.getFirstDepartureTime();
        assertEquals(new LocalTime(8,00), departTime);

        assertEquals(22, walkingStage.getDuration());

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
