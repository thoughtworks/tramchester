package com.tramchester.integration.repository.rail;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.rail.IntegrationRailTestConfig;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.StopCallRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.TrainTest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.TestEnv.assertMinutesEquals;
import static org.junit.jupiter.api.Assertions.*;

@TrainTest
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
public class StopCallRepositoryRailTest {
    private static ComponentContainer componentContainer;

    private StopCallRepository stopCallRepository;
    private StationRepository stationRepository;
    private RouteRepository routeRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        componentContainer = new ComponentsBuilder().create(new IntegrationRailTestConfig(), TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTest() {
        stopCallRepository = componentContainer.get(StopCallRepository.class);
        stationRepository = componentContainer.get(StationRepository.class);
        routeRepository = componentContainer.get(RouteRepository.class);
    }

    @Test
    void shouldReproIssueWithCrossingMidnight() {
        Route route = routeRepository.getRouteById(StringIdFor.createId("SR:PERTH=>EDINBUR:5"));

        Station inverkeithing = stationRepository.getStationById(StringIdFor.createId("IVRKTHG"));
        Station haymarket = stationRepository.getStationById(StringIdFor.createId("HAYMRKT"));

        StopCallRepository.Costs costs = stopCallRepository.getCostsBetween(route, inverkeithing, haymarket);

        assertFalse(costs.isEmpty());

        // was getting costs > 23 hours due to crossing midnight
        assertEquals(Duration.ofMinutes(14).plusSeconds(36), costs.average(), costs.toString());
        assertMinutesEquals(16, costs.max(), costs.toString());
    }

    @Disabled("Data does contain a zero cost trip X13514:20220124:20220127")
    @Test
    void shouldReproIssueWithIncorrectZeroCosts() {
        Station mulsecoomb = stationRepository.getStationById(StringIdFor.createId("MLSECMB"));
        Station londonRoadBrighton = stationRepository.getStationById(StringIdFor.createId("BRGHLRD"));

        Set<Route> calling = routeRepository.getRoutes().stream().
                flatMap(route -> route.getTrips().stream()).
                filter(trip -> trip.getStopCalls().callsAt(mulsecoomb) && trip.getStopCalls().callsAt(londonRoadBrighton)).
                filter(trip -> isBefore(trip, mulsecoomb, londonRoadBrighton)).
                map(Trip::getRoute).
                collect(Collectors.toSet());

        assertFalse(calling.isEmpty());

        calling.forEach(route -> {
            StopCallRepository.Costs costs = stopCallRepository.getCostsBetween(route, mulsecoomb, londonRoadBrighton);
            assertNotEquals(Duration.ZERO, costs.average(), costs.toString() + route);
            assertNotEquals(Duration.ZERO, costs.max(), costs.toString() + route);
        });
    }

    private boolean isBefore(Trip trip, Station stationA, Station stationB) {
        final boolean includeNotStopping = true;
        return trip.getStopCalls().getStationSequence(includeNotStopping).
                indexOf(stationA) < trip.getStopCalls().getStationSequence(includeNotStopping).indexOf(stationB);
    }

}
