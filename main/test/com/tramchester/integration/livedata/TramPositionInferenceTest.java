package com.tramchester.integration.livedata;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.StationPair;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.RouteReachable;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.livedata.LiveDataUpdater;
import com.tramchester.livedata.TramPosition;
import com.tramchester.livedata.TramPositionInference;
import com.tramchester.livedata.repository.DueTramsRepository;
import com.tramchester.livedata.repository.DueTramsSource;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TramStationAdjacenyRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.LiveDataMessagesCategory;
import com.tramchester.testSupport.testTags.LiveDataTestCategory;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class TramPositionInferenceTest {

    private static ComponentContainer componentContainer;
    private static IntegrationTramTestConfig testConfig;

    private TramPositionInference positionInference;
    private StationRepository stationRepository;
    private TramServiceDate date;
    private TramTime time;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        testConfig = new IntegrationTramTestConfig(true);
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        ProvidesNow providesLocalNow = new ProvidesLocalNow();

        LiveDataUpdater liveDataSource = componentContainer.get(LiveDataUpdater.class);
        liveDataSource.refreshRespository();
        RouteReachable routeReachable = componentContainer.get(RouteReachable.class);
        TramStationAdjacenyRepository adjacenyMatrix = componentContainer.get(TramStationAdjacenyRepository.class);
        DueTramsSource dueTramsRepo = componentContainer.get(DueTramsRepository.class);

        positionInference = new TramPositionInference(dueTramsRepo, adjacenyMatrix, routeReachable);
        stationRepository = componentContainer.get(StationRepository.class);
        date = TramServiceDate.of(providesLocalNow.getDate());
        time = providesLocalNow.getNowHourMins();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @Test
    @LiveDataMessagesCategory
    void needApiKeyPresentToFetchData() {
        assertNotNull(testConfig.getLiveDataConfig().getDataSubscriptionKey(), "subscription key null");
        assertFalse(testConfig.getLiveDataConfig().getDataSubscriptionKey().isEmpty(), "no subscription key present");
    }

    @Test
    @LiveDataMessagesCategory
    @Disabled("needs a tram in the right place at the right time")
    void shouldInferTramPosition() {
        // NOTE: costs are not symmetric between two stations, i.e. one direction might cost more than the other
        // Guess: this is down to signalling, track, etc.

        Duration cost = Duration.ofMinutes(3); // cost between the stations, no due trams outside this limit should appear

        Station first = stationRepository.getStationById(TramStations.Deansgate.getId());
        Station second = stationRepository.getStationById(TramStations.Cornbrook.getId());

        StationPair pair = StationPair.of(first, second);
        TramPosition between = positionInference.findBetween(pair, date, time);
        assertEquals(first, between.getFirst());
        assertEquals(second, between.getSecond());
        assertTrue(between.getTrams().size()>=1, "trams between");
        assertEquals(cost, between.getCost());
        between.getTrams().
                forEach(dueTram -> assertFalse((dueTram.getWait().compareTo(cost)) > 0, dueTram.getWait().toString()));

        TramPosition otherDirection = positionInference.findBetween(pair, date, time);
        assertTrue(otherDirection.getTrams().size()>=1, "no trams in other direction");
        assertEquals(cost, between.getCost());
        otherDirection.getTrams().
                forEach(dueTram -> assertFalse((dueTram.getWait().compareTo(cost)) > 0, dueTram.getWait().toString()));
    }

    @Test
    @LiveDataTestCategory
    void shouldHaveSomeTramsPresentInNetwork() {
        List<TramPosition> results = positionInference.inferWholeNetwork(date, time).stream().
                filter(result -> !result.getTrams().isEmpty()).collect(Collectors.toList());

        assertFalse(results.isEmpty());

    }
}
