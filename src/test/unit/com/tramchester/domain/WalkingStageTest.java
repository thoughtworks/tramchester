package com.tramchester.domain;


import com.tramchester.Stations;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.graph.Nodes.QueryNode;
import com.tramchester.graph.Nodes.TramNode;
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
        walkingStage = new WalkingStage(cost, start, destination, begin);
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
        assertEquals(start, walkingStage.getFirstStation());
        assertEquals(destination, walkingStage.getLastStation());
    }

    @Test
    public void shouldGetWalkingDestinationAsFirstStationWhenStartingFromMyLocation() {
        WalkingStage stage = new WalkingStage(10, new MyLocation(new LatLong(-2,1)), Stations.Victoria, 8*60);
        assertEquals(Stations.Victoria, stage.getFirstStation());
    }
}
