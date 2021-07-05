package com.tramchester.integration.graph.buses;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Agency;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.CompositeStation;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.search.RouteToRouteCosts;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.repository.CompositeStationRepository;
import com.tramchester.repository.RouteRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.BusStations;
import com.tramchester.testSupport.testTags.BusTest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;


@BusTest
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
public class BusRouteToRouteCostsTest {

    private static ComponentContainer componentContainer;

    private RouteToRouteCosts routeToRouteCosts;
    private RouteRepository routeRepository;
    private CompositeStationRepository stationRepository;

    @BeforeAll
    static void onceBeforeAnyTestRuns() {
        TramchesterConfig config = new IntegrationBusTestConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        routeToRouteCosts = componentContainer.get(RouteToRouteCosts.class);
        routeRepository = componentContainer.get(RouteRepository.class);
        stationRepository = componentContainer.get(CompositeStationRepository.class)
;    }

    // For testing, likely to vary a lot with timetable updates
    @Disabled("Changes too often to be useful")
    @Test
    void shouldHaveExpectedNumber() {
        assertEquals(1827904, routeToRouteCosts.size());
    }

    @Test
    void shouldGetNumberOfRouteHopsBetweenAltrinchamStockport() {
        CompositeStation start = stationRepository.findByName("Altrincham Interchange");
        CompositeStation end = stationRepository.findByName("Stockport Bus Station");

        assertEquals(0, routeToRouteCosts.minRouteHops(start, end));
    }

    @Test
    void shouldGetNumberOfRouteHopsBetweenAltrinchamShudehill() {
        CompositeStation start = stationRepository.findByName("Altrincham Interchange");
        CompositeStation end = stationRepository.findByName("Shudehill Interchange");

        assertEquals(1, routeToRouteCosts.minRouteHops(start, end));
        assertEquals(3, routeToRouteCosts.maxRouteHops(start, end));
    }

    @Test
    void shouldGetNumberOfRouteHopsBetweenKnutsfordAndShudehill() {
        Station start = stationRepository.getStationById(BusStations.KnutsfordStationStand3.getId());
        CompositeStation end = stationRepository.findByName("Shudehill Interchange");

        assertEquals(2, routeToRouteCosts.minRouteHops(start, end));
        assertEquals(3, routeToRouteCosts.maxRouteHops(start, end));
    }

    @Test
    void shouldHaveCorrectCostBetweenRoutesDiffDirections() {
        IdFor<Agency> agencyId = StringIdFor.createId("DAGC");
        Set<Route> altyToKnutsford = routeRepository.findRoutesByName(agencyId,
                "Altrincham - Wilmslow - Knutsford - Macclesfield");
        assertEquals(1, altyToKnutsford.size());
        Set<Route> knutsfordToAlty = routeRepository.findRoutesByName(agencyId,
                "Macclesfield - Knutsford - Wilmslow - Altrincham");
        assertEquals(1, knutsfordToAlty.size());

        assertEquals(1, routeToRouteCosts.getFor(altyToKnutsford.iterator().next(), knutsfordToAlty.iterator().next()));

    }

}
