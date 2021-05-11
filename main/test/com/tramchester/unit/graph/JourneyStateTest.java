package com.tramchester.unit.graph;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.SortsPositions;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.search.JourneyState;
import com.tramchester.graph.search.stateMachine.RegistersStates;
import com.tramchester.graph.search.stateMachine.TraversalOps;
import com.tramchester.graph.search.stateMachine.states.TraversalStateFactory;
import com.tramchester.graph.search.stateMachine.states.NotStartedState;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TripRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TestStation;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.reference.TramTransportDataForTestFactory;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class JourneyStateTest extends EasyMockSupport {

    private TramTime queryTime;
    private NotStartedState traversalState;

    // TODO ON BUS

    @BeforeEach
    void onceBeforeEachTestRuns() {
        LatLong latLongHint = TramStations.ManAirport.getLatLong();
        Set<Long> destinationNodeIds = new HashSet<>();
        destinationNodeIds.add(42L);

        Station station = TestStation.forTest("destinationStationId", "area", "name", new LatLong(1,1), TransportMode.Tram);
        station.addRoute(TestEnv.getTramTestRoute());
        
        Set<Station> destinations = Collections.singleton(station);
        ProvidesNow providesNow = new ProvidesLocalNow();
        StationRepository repository = new TramTransportDataForTestFactory(providesNow).getData();
        SortsPositions sortsPositions = new SortsPositions(repository);

        TripRepository tripRepository = createMock(TripRepository.class);
        NodeContentsRepository cachedNodeOperations = createMock(NodeContentsRepository.class);
        final TramchesterConfig config = TestEnv.GET();

        RegistersStates registersStates = new RegistersStates();
        TraversalStateFactory traversalStateFactory = new TraversalStateFactory(registersStates, config);
        traversalState = new NotStartedState(new TraversalOps(cachedNodeOperations, tripRepository, sortsPositions, destinations,
                destinationNodeIds, latLongHint), traversalStateFactory);
        queryTime = TramTime.of(9, 15);
    }

    @Test
    void shouldBeginJourney() {
        JourneyState state = new JourneyState(queryTime, traversalState);
        assertFalse(TransportMode.isTram(state));

        int currentCost = 0;
        state.updateJourneyClock(currentCost);
        assertEquals(queryTime, state.getJourneyClock());
        assertFalse(TransportMode.isTram(state));

        currentCost = 14;
        state.updateJourneyClock(currentCost);
        assertEquals(TramTime.of(9,29), state.getJourneyClock());
        assertFalse(TransportMode.isTram(state));

    }

    @Test
    void shouldBoardATram() throws TramchesterException {
        JourneyState state = new JourneyState(queryTime, traversalState);
        assertFalse(TransportMode.isTram(state));
        assertFalse(state.hasBegunJourney());

        int currentCost = 10;
        TramTime boardingTime = TramTime.of(9, 30);
        state.board(TransportMode.Tram);
        assertTrue(state.hasBegunJourney());
        state.recordVehicleDetails(boardingTime,currentCost);

        assertTrue(TransportMode.isTram(state));
        assertEquals(boardingTime, state.getJourneyClock());
    }

    @Test
    void shouldConnection() {
        JourneyState state = new JourneyState(queryTime, traversalState);
        assertFalse(TransportMode.isTram(state));

        state.walkingConnection();
        assertEquals(1, state.getNumberWalkingConnections());

        state.walkingConnection();
        assertEquals(2, state.getNumberWalkingConnections());
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
        assertTrue(state.hasBegunJourney());

        state.recordVehicleDetails(boardingTime,currentCost);
        state.leave(TransportMode.Tram, 20);
        assertTrue(state.hasBegunJourney());

        assertThrows(TramchesterException.class, () -> state.leave(TransportMode.Tram, 25));
    }

    @Test
    void shouldHaveCorrectTripIdAndClockDuringATrip() throws TramchesterException {
        JourneyState state = new JourneyState(queryTime, traversalState);

        TramTime boardingTime = TramTime.of(9, 30);
        state.board(TransportMode.Tram);
        state.recordVehicleDetails(boardingTime,10);
        assertEquals(boardingTime, state.getJourneyClock());

        state.updateJourneyClock(15); // 15 - 10
        assertEquals(boardingTime.plusMinutes(5), state.getJourneyClock());

        state.updateJourneyClock(20);  // 20 - 10
        assertEquals(boardingTime.plusMinutes(10), state.getJourneyClock());
    }

    @Test
    void shouldHaveCorrectTimeWhenDepartingTram() throws TramchesterException {
        JourneyState state = new JourneyState(queryTime, traversalState);
        assertFalse(TransportMode.isTram(state));

        state.board(TransportMode.Tram);
        state.recordVehicleDetails(TramTime.of(9,30),10);         // 10 mins cost
        assertTrue(TransportMode.isTram(state));

        state.leave(TransportMode.Tram, 25);                            // 25 mins cost, offset is 15 mins
        assertEquals(TramTime.of(9,45), state.getJourneyClock()); // should be depart tram time
        assertFalse(TransportMode.isTram(state));

        state.updateJourneyClock(35);
        assertEquals(TramTime.of(9,55), state.getJourneyClock()); // i.e not just queryTime + 35 minutes
    }

    @Test
    void shouldHaveCorrectTimeWhenDepartingAndBoardingTram() throws TramchesterException {
        JourneyState state = new JourneyState(queryTime, traversalState);

        state.board(TransportMode.Tram);
        state.recordVehicleDetails(TramTime.of(9,30),10);         // 10 mins cost

        state.leave(TransportMode.Tram, 25);                            // 25 mins cost, offset is 15 mins
        assertEquals(TramTime.of(9,45), state.getJourneyClock()); // should be depart tram time

        state.board(TransportMode.Tram);
        state.recordVehicleDetails(TramTime.of(9,50),25);
        assertEquals(TramTime.of(9,50), state.getJourneyClock()); // should be depart tram time

        state.leave(TransportMode.Tram, 35);                            // 35-25 = 10 mins
        assertEquals(TramTime.of(10,0), state.getJourneyClock());
    }

    @Test
    void shouldCreateNewState() throws TramchesterException {
        JourneyState journeyState = new JourneyState(TramTime.of(7,55), traversalState);
        journeyState.walkingConnection();

        JourneyState newStateA = JourneyState.fromPrevious(journeyState);
        assertEquals(TramTime.of(7,55), journeyState.getJourneyClock());
        assertFalse(TransportMode.isTram(newStateA));
        assertFalse(newStateA.hasBegunJourney());
        assertEquals(0, newStateA.getNumberChanges());
        assertEquals(1, newStateA.getNumberWalkingConnections());

        newStateA.board(TransportMode.Tram);
        newStateA.recordVehicleDetails(TramTime.of(8,15), 15);
        assertEquals(TramTime.of(8,15), newStateA.getJourneyClock());
        newStateA.walkingConnection();

        JourneyState newStateB = JourneyState.fromPrevious(newStateA);
        assertTrue(newStateB.hasBegunJourney());

        assertEquals(TramTime.of(8,15), newStateB.getJourneyClock());
        assertTrue(TransportMode.isTram(newStateB));
        newStateB.leave(TransportMode.Tram, 20);
        newStateB.board(TransportMode.Tram);
        assertEquals(2, newStateB.getNumberWalkingConnections());
        assertEquals(1, newStateB.getNumberChanges());
    }

}
