package com.tramchester.integration.repository.rail;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.dataimport.rail.reference.TrainOperatingCompanies;
import com.tramchester.domain.Agency;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.rail.IntegrationRailTestConfig;
import com.tramchester.repository.RouteRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.TrainTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.integration.testSupport.rail.RailStationIds.LondonEuston;
import static com.tramchester.integration.testSupport.rail.RailStationIds.ManchesterPiccadilly;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TrainTest
public class RouteRepositoryRailTest {
    private static ComponentContainer componentContainer;
    private RouteRepository routeRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        IntegrationRailTestConfig config = new IntegrationRailTestConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        routeRepository = componentContainer.get(RouteRepository.class);
    }

    @Test
    void shouldHaveExpectedNumberOfRailRoutes() {
        Set<Route> allRoutes = routeRepository.getRoutes();

        assertEquals(6944, allRoutes.size());
    }

    @Test
    void shouldHaveExpectedNumberManchesterToLondonRoutes() {
        IdFor<Agency> agencyId = Agency.createId(TrainOperatingCompanies.VT.name());

        List<Route> result = routeRepository.getRoutes().stream().
                filter(route -> route.getAgency().getId().equals(agencyId)).
                filter(route -> tripsMatch(route, ManchesterPiccadilly.getId(), LondonEuston.getId())).
                collect(Collectors.toList());

        assertEquals(9, result.size(), HasId.asIds(result));
    }

    private boolean tripsMatch(Route route, IdFor<Station> first, IdFor<Station> last) {
        return route.getTrips().stream().
                map(Trip::getStopCalls).
                filter(stopCalls -> stopCalls.getFirstStop().getStationId().equals(first)).
                anyMatch(stopCalls -> stopCalls.getLastStop().getStationId().equals(last));
    }
}
