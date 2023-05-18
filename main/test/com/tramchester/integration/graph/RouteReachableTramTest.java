package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Route;
import com.tramchester.domain.StationPair;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.RouteReachable;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.TestEnv.Modes.TramsOnly;
import static com.tramchester.testSupport.reference.KnownTramRoute.AltrinchamManchesterBury;
import static com.tramchester.testSupport.reference.KnownTramRoute.AltrinchamPiccadilly;
import static com.tramchester.testSupport.reference.TramStations.Altrincham;
import static com.tramchester.testSupport.reference.TramStations.NavigationRoad;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteReachableTramTest {
    private static ComponentContainer componentContainer;

    private RouteReachable reachable;
    private StationRepository stationRepository;

    @BeforeAll
    static void onceBeforeAnyTestRuns() {
        TramchesterConfig config = new IntegrationTramTestConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        stationRepository = componentContainer.get(StationRepository.class);
        reachable = componentContainer.get(RouteReachable.class);
    }

    @Test
    void shouldTestGetRoutesFromStartToNeighbour() {

        TramDate when = TestEnv.testDay();
        Station start = stationRepository.getStationById(Altrincham.getId());
        Station next = stationRepository.getStationById(NavigationRoad.getId());

        TimeRange timeRange = TimeRange.of(TramTime.of(8,30), Duration.ofMinutes(30), Duration.ofMinutes(30));
        List<Route> results = reachable.getRoutesFromStartToNeighbour(StationPair.of(start, next), when, timeRange, TramsOnly);

        Set<String> names = results.stream().map(Route::getName).collect(Collectors.toSet());

        assertEquals(2, names.size(), names.toString());

        assertTrue(names.contains(AltrinchamManchesterBury.longName()), names.toString());
        assertTrue(names.contains(AltrinchamPiccadilly.longName()), names.toString());
    }


}
