package com.tramchester.unit.graph;

import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.CachedNodeOperations;
import com.tramchester.graph.NodeIdLabelMap;
import com.tramchester.graph.search.JourneyState;
import com.tramchester.graph.states.NotStartedState;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertThrows;

class JourneyStateTest {

    private TramTime queryTime;
    private NotStartedState traversalState;

    // TODO ON BUS

    @BeforeEach
    void onceBeforeEachTestRuns() {
        List<String> destinationStationIds = Collections.singletonList("destinationStationId");
        traversalState = new NotStartedState(new CachedNodeOperations(new NodeIdLabelMap()),
                42, destinationStationIds, TestEnv.GET());
        queryTime = TramTime.of(9, 15);
    }

    @Test
    void shouldBeginJourney() {
        JourneyState state = new JourneyState(queryTime, traversalState);
        Assertions.assertFalse(state.onTram());

        int currentCost = 0;
        state.updateJourneyClock(currentCost);
        Assertions.assertEquals(queryTime, state.getJourneyClock());
        Assertions.assertFalse(state.onTram());

        currentCost = 14;
        state.updateJourneyClock(currentCost);
        Assertions.assertEquals(TramTime.of(9,29), state.getJourneyClock());
        Assertions.assertFalse(state.onTram());

    }

    @Test
    void shouldBoardATram() throws TramchesterException {
        JourneyState state = new JourneyState(queryTime, traversalState);
        Assertions.assertFalse(state.onTram());

        int currentCost = 10;
        TramTime boardingTime = TramTime.of(9, 30);
        state.boardTram();
        state.recordTramDetails(boardingTime,currentCost);

        Assertions.assertTrue(state.onTram());
        Assertions.assertEquals(boardingTime, state.getJourneyClock());
//        assertEquals("tripId1", state.getTripId());
    }

    @Test
    void shouldNotBoardATramIfAlreadyOnATram() throws TramchesterException {
        JourneyState state = new JourneyState(queryTime, traversalState);

        state.boardTram();
        assertThrows(TramchesterException.class, state::boardTram);
    }

    @Test
    void shouldNotLeaveATramIfAlreadyOffATram() throws TramchesterException {
        JourneyState state = new JourneyState(queryTime, traversalState);
        TramTime boardingTime = TramTime.of(9, 30);

        int currentCost = 14;
        state.boardTram();
        state.recordTramDetails(boardingTime,currentCost);
        state.leaveTram(20);

        assertThrows(TramchesterException.class, () -> state.leaveTram(25));
    }

    @Test
    void shouldHaveCorrectTripIdAndClockDuringATrip() throws TramchesterException {
        JourneyState state = new JourneyState(queryTime, traversalState);

        TramTime boardingTime = TramTime.of(9, 30);
        state.boardTram();
        state.recordTramDetails(boardingTime,10);
        Assertions.assertEquals(boardingTime, state.getJourneyClock());

        state.updateJourneyClock(15); // 15 - 10
        Assertions.assertEquals(boardingTime.plusMinutes(5), state.getJourneyClock());

        state.updateJourneyClock(20);  // 20 - 10
        Assertions.assertEquals(boardingTime.plusMinutes(10), state.getJourneyClock());
    }

    @Test
    void shouldHaveCorrectTimeWhenDepartingTram() throws TramchesterException {
        JourneyState state = new JourneyState(queryTime, traversalState);
        Assertions.assertFalse(state.onTram());

        state.boardTram();
        state.recordTramDetails(TramTime.of(9,30),10);         // 10 mins cost
        Assertions.assertTrue(state.onTram());
//        assertEquals("tripId1", state.getTripId());

        state.leaveTram(25);                            // 25 mins cost, offset is 15 mins
        Assertions.assertEquals(TramTime.of(9,45), state.getJourneyClock()); // should be depart tram time
        Assertions.assertFalse(state.onTram());
//        assertEquals("", state.getTripId());

        state.updateJourneyClock(35);
        Assertions.assertEquals(TramTime.of(9,55), state.getJourneyClock()); // i.e not just queryTime + 35 minutes
    }

    @Test
    void shouldHaveCorrectTimeWhenDepartingAndBoardingTram() throws TramchesterException {
        JourneyState state = new JourneyState(queryTime, traversalState);

        state.boardTram();
        state.recordTramDetails(TramTime.of(9,30),10);         // 10 mins cost
//        assertEquals("tripId1", state.getTripId());

        state.leaveTram(25);                            // 25 mins cost, offset is 15 mins
        Assertions.assertEquals(TramTime.of(9,45), state.getJourneyClock()); // should be depart tram time

        state.boardTram();
        state.recordTramDetails(TramTime.of(9,50),25);
        Assertions.assertEquals(TramTime.of(9,50), state.getJourneyClock()); // should be depart tram time
//        assertEquals("tripId2", state.getTripId());

        state.leaveTram(35);                            // 35-25 = 10 mins
        Assertions.assertEquals(TramTime.of(10,0), state.getJourneyClock());
    }

    @Test
    void shouldCreateNewState() throws TramchesterException {
        JourneyState journeyState = new JourneyState(TramTime.of(7,55), traversalState);

        JourneyState newStateA = JourneyState.fromPrevious(journeyState);
        Assertions.assertEquals(TramTime.of(7,55), journeyState.getJourneyClock());
        Assertions.assertFalse(journeyState.onTram());
//        assertTrue(journeyState.getTripId().isEmpty());

        newStateA.boardTram();
        newStateA.recordTramDetails(TramTime.of(8,15), 15);
        Assertions.assertEquals(TramTime.of(8,15), newStateA.getJourneyClock());

        JourneyState newStateB = JourneyState.fromPrevious(newStateA);
        Assertions.assertEquals(TramTime.of(8,15), newStateB.getJourneyClock());
        Assertions.assertTrue(newStateB.onTram());
//        assertEquals("tripId1", newStateB.getTripId());
    }
}
