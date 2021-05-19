package com.tramchester.integration.livedata;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.StationPair;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.RouteReachable;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.livedata.LiveDataUpdater;
import com.tramchester.livedata.TramPosition;
import com.tramchester.livedata.TramPositionInference;
import com.tramchester.repository.*;
import com.tramchester.testSupport.testTags.LiveDataMessagesCategory;
import com.tramchester.testSupport.testTags.LiveDataTestCategory;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TramPositionInferenceTest {

    private static ComponentContainer componentContainer;

    private TramPositionInference positionInference;
    private StationRepository stationRepository;
    private TramServiceDate date;
    private TramTime time;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        IntegrationTramTestConfig testConfig = new IntegrationTramTestConfig(true);
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        ProvidesLocalNow providesLocalNow = new ProvidesLocalNow();

        LiveDataUpdater liveDataSource = componentContainer.get(LiveDataUpdater.class);
        liveDataSource.refreshRespository();
        RouteReachable routeReachable = componentContainer.get(RouteReachable.class);
        TramStationAdjacenyRepository adjacenyMatrix = componentContainer.get(TramStationAdjacenyRepository.class);
        DueTramsSource dueTramsRepo = componentContainer.get(DueTramsRepository.class);

        positionInference = new TramPositionInference(dueTramsRepo, adjacenyMatrix, routeReachable);
        stationRepository = componentContainer.get(StationRepository.class);
        date = TramServiceDate.of(providesLocalNow.getDate());
        time = providesLocalNow.getNow();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    // TODO rework this test to account for new 12 minutes frequency which is causing frequent intermittent failures
    @Test
    @LiveDataMessagesCategory
    void shouldInferTramPosition() {
        // NOTE: costs are not symmetric between two stations, i.e. one direction might cost more than the other
        // Guess this is down to signalling, track, etc.
        int cost = 3; // cost between the stations, no due trams outside this limit should appear

        Station first = stationRepository.getStationById(TramStations.Deansgate.getId());
        Station second = stationRepository.getStationById(TramStations.Cornbrook.getId());

        StationPair pair = StationPair.of(first, second);
        TramPosition between = positionInference.findBetween(pair, date, time);
        assertEquals(first, between.getFirst());
        assertEquals(second, between.getSecond());
        assertTrue(between.getTrams().size()>=1, "trams between");
        assertEquals(cost, between.getCost());
        between.getTrams().forEach(dueTram -> assertFalse((dueTram.getWait())> cost, Integer.toString(dueTram.getWait())));

        TramPosition otherDirection = positionInference.findBetween(pair, date, time);
        assertTrue(otherDirection.getTrams().size()>=1, "no trams in other direction");
        assertEquals(cost, between.getCost());
        otherDirection.getTrams().forEach(dueTram -> assertFalse((dueTram.getWait())> cost, Integer.toString(dueTram.getWait())));
    }

    @Test
    @LiveDataTestCategory
    void shouldInferAllTramPositions() {
        List<TramPosition> results = positionInference.inferWholeNetwork(date, time);
        long hasTrams = results.stream().filter(position -> !position.getTrams().isEmpty()).count();

        assertTrue(hasTrams>0);
    }
}
