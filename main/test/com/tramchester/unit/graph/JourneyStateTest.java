package com.tramchester.unit.graph;

import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.SortsPositions;
import com.tramchester.geo.StationLocations;
import com.tramchester.graph.CachedNodeOperations;
import com.tramchester.graph.search.JourneyState;
import com.tramchester.graph.search.states.NotStartedState;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TestStation;
import com.tramchester.testSupport.TransportDataForTestFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opengis.referencing.operation.TransformException;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;

class JourneyStateTest {

    private TramTime queryTime;
    private NotStartedState traversalState;

    // TODO ON BUS

    @BeforeEach
    void onceBeforeEachTestRuns() throws TransformException {
        LatLong latLongHint = TestEnv.manAirportLocation;
        Set<Long> destinationNodeIds = new HashSet<>();
        destinationNodeIds.add(42L);

        Station station = TestStation.forTest("destinationStationId", "area", "name", new LatLong(1,1), TransportMode.Tram);
        station.addRoute(TestEnv.getTestRoute());
        
        Set<Station> destinations = Collections.singleton(station);
        StationLocations locations = new StationLocations();
        ProvidesNow providesNow = new ProvidesLocalNow();
        StationRepository repository = new TransportDataForTestFactory(locations, providesNow).get();
        SortsPositions sortsPositions = new SortsPositions(repository);

        traversalState = new NotStartedState(sortsPositions, new CachedNodeOperations(),
                destinationNodeIds, destinations, latLongHint, TestEnv.GET());
        queryTime = TramTime.of(9, 15);
    }

    @Test
    void shouldBeginJourney() {
        JourneyState state = new JourneyState(queryTime, traversalState);
        Assertions.assertFalse(TransportMode.isTram(state));

        int currentCost = 0;
        state.updateJourneyClock(currentCost);
        Assertions.assertEquals(queryTime, state.getJourneyClock());
        Assertions.assertFalse(TransportMode.isTram(state));

        currentCost = 14;
        state.updateJourneyClock(currentCost);
        Assertions.assertEquals(TramTime.of(9,29), state.getJourneyClock());
        Assertions.assertFalse(TransportMode.isTram(state));

    }

    @Test
    void shouldBoardATram() throws TramchesterException {
        JourneyState state = new JourneyState(queryTime, traversalState);
        Assertions.assertFalse(TransportMode.isTram(state));

        int currentCost = 10;
        TramTime boardingTime = TramTime.of(9, 30);
        state.board(TransportMode.Tram);
        state.recordVehicleDetails(boardingTime,currentCost);

        Assertions.assertTrue(TransportMode.isTram(state));
        Assertions.assertEquals(boardingTime, state.getJourneyClock());
//        assertEquals("tripId1", state.getTripId());
    }

    @Test
    void shouldNotBoardATramIfAlreadyOnATram() throws TramchesterException {
        JourneyState state = new JourneyState(queryTime, traversalState);

        state.board(TransportMode.Tram);
        assertThrows(TramchesterException.class, () -> state.board(TransportMode.Tram));
    }

    @Test
    void shouldNotLeaveATramIfAlreadyOffATram() throws TramchesterException {
        JourneyState state = new JourneyState(queryTime, traversalState);
        TramTime boardingTime = TramTime.of(9, 30);

        int currentCost = 14;
        state.board(TransportMode.Tram);
        state.recordVehicleDetails(boardingTime,currentCost);
        state.leave(TransportMode.Tram, 20);

        assertThrows(TramchesterException.class, () -> state.leave(TransportMode.Tram, 25));
    }

    @Test
    void shouldHaveCorrectTripIdAndClockDuringATrip() throws TramchesterException {
        JourneyState state = new JourneyState(queryTime, traversalState);

        TramTime boardingTime = TramTime.of(9, 30);
        state.board(TransportMode.Tram);
        state.recordVehicleDetails(boardingTime,10);
        Assertions.assertEquals(boardingTime, state.getJourneyClock());

        state.updateJourneyClock(15); // 15 - 10
        Assertions.assertEquals(boardingTime.plusMinutes(5), state.getJourneyClock());

        state.updateJourneyClock(20);  // 20 - 10
        Assertions.assertEquals(boardingTime.plusMinutes(10), state.getJourneyClock());
    }

    @Test
    void shouldHaveCorrectTimeWhenDepartingTram() throws TramchesterException {
        JourneyState state = new JourneyState(queryTime, traversalState);
        Assertions.assertFalse(TransportMode.isTram(state));

        state.board(TransportMode.Tram);
        state.recordVehicleDetails(TramTime.of(9,30),10);         // 10 mins cost
        Assertions.assertTrue(TransportMode.isTram(state));
//        assertEquals("tripId1", state.getTripId());

        state.leave(TransportMode.Tram, 25);                            // 25 mins cost, offset is 15 mins
        Assertions.assertEquals(TramTime.of(9,45), state.getJourneyClock()); // should be depart tram time
        Assertions.assertFalse(TransportMode.isTram(state));
//        assertEquals("", state.getTripId());

        state.updateJourneyClock(35);
        Assertions.assertEquals(TramTime.of(9,55), state.getJourneyClock()); // i.e not just queryTime + 35 minutes
    }

    @Test
    void shouldHaveCorrectTimeWhenDepartingAndBoardingTram() throws TramchesterException {
        JourneyState state = new JourneyState(queryTime, traversalState);

        state.board(TransportMode.Tram);
        state.recordVehicleDetails(TramTime.of(9,30),10);         // 10 mins cost
//        assertEquals("tripId1", state.getTripId());

        state.leave(TransportMode.Tram, 25);                            // 25 mins cost, offset is 15 mins
        Assertions.assertEquals(TramTime.of(9,45), state.getJourneyClock()); // should be depart tram time

        state.board(TransportMode.Tram);
        state.recordVehicleDetails(TramTime.of(9,50),25);
        Assertions.assertEquals(TramTime.of(9,50), state.getJourneyClock()); // should be depart tram time
//        assertEquals("tripId2", state.getTripId());

        state.leave(TransportMode.Tram, 35);                            // 35-25 = 10 mins
        Assertions.assertEquals(TramTime.of(10,0), state.getJourneyClock());
    }

    @Test
    void shouldCreateNewState() throws TramchesterException {
        JourneyState journeyState = new JourneyState(TramTime.of(7,55), traversalState);

        JourneyState newStateA = JourneyState.fromPrevious(journeyState);
        Assertions.assertEquals(TramTime.of(7,55), journeyState.getJourneyClock());
        Assertions.assertFalse(TransportMode.isTram(newStateA));
//        assertTrue(journeyState.getTripId().isEmpty());

        newStateA.board(TransportMode.Tram);
        newStateA.recordVehicleDetails(TramTime.of(8,15), 15);
        Assertions.assertEquals(TramTime.of(8,15), newStateA.getJourneyClock());

        JourneyState newStateB = JourneyState.fromPrevious(newStateA);
        Assertions.assertEquals(TramTime.of(8,15), newStateB.getJourneyClock());
        Assertions.assertTrue(TransportMode.isTram(newStateB));
//        assertEquals("tripId1", newStateB.getTripId());
    }
}
