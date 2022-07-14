package com.tramchester.integration.graph.buses;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Agency;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.NumberOfChanges;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.search.RouteToRouteCosts;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.StationGroupsRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.BusStations;
import com.tramchester.testSupport.reference.BusStations.Composites;
import com.tramchester.testSupport.testTags.BusTest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;


@BusTest
public class BusRouteToRouteCostsTest {

    private static ComponentContainer componentContainer;

    private RouteToRouteCosts routeToRouteCosts;
    private RouteRepository routeRepository;
    private StationGroupsRepository stationGroupsRepository;
    private StationRepository stationRepository;
    private LocalDate date;

    @BeforeAll
    static void onceBeforeAnyTestRuns() {
        TramchesterConfig config = new IntegrationBusTestConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
        TestEnv.clearDataCache(componentContainer);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        routeToRouteCosts = componentContainer.get(RouteToRouteCosts.class);
        routeRepository = componentContainer.get(RouteRepository.class);
        stationGroupsRepository = componentContainer.get(StationGroupsRepository.class);
        stationRepository = componentContainer.get(StationRepository.class);

        date = TestEnv.testDay();
;    }

    // For testing, likely to vary a lot with timetable updates
    @Disabled("Changes too often to be useful")
    @Test
    void shouldHaveExpectedNumber() {
        assertEquals(1827904, routeToRouteCosts.size());
    }

    @Test
    void shouldGetNumberOfRouteHopsBetweenAltrinchamStockport() {
        StationGroup start = stationGroupsRepository.findByName(Composites.AltrinchamInterchange.getName());
        StationGroup end = stationGroupsRepository.findByName(Composites.StockportTempBusStation.getName());

        // one for the temp stockport bus station, was zero, seems direct alty buses terminating somewhere else
        assertEquals(1, routeToRouteCosts.getNumberOfChanges(start, end, date).getMin());
    }

    @Test
    void shouldGetNumberOfRouteHopsBetweenAltrinchamShudehill() {
        StationGroup start = stationGroupsRepository.findByName(Composites.AltrinchamInterchange.getName());
        StationGroup end = stationGroupsRepository.findByName("Shudehill Interchange");

        NumberOfChanges numberOfChanges = routeToRouteCosts.getNumberOfChanges(start, end, date);
        assertEquals(1, numberOfChanges.getMin());
        assertEquals(3, numberOfChanges.getMax());
    }

    @Test
    void shouldGetNumberOfRouteHopsBetweenKnutsfordAndShudehill() {
        Station start = stationRepository.getStationById(BusStations.KnutsfordStationStand3.getId());
        StationGroup end = stationGroupsRepository.findByName("Shudehill Interchange");

        NumberOfChanges numberOfChanges = routeToRouteCosts.getNumberOfChanges(LocationSet.singleton(start), LocationSet.of(end.getContained()), date);

        assertEquals(2, numberOfChanges.getMin());
        assertEquals(3, numberOfChanges.getMax());
    }

    @Test
    void shouldHaveCorrectCostBetweenRoutesDiffDirections() {
        IdFor<Agency> agencyId = StringIdFor.createId("DAGC");
        Set<Route> altyToKnutsford = routeRepository.findRoutesByName(agencyId,
                "Altrincham - Macclesfield");
        assertEquals(2, altyToKnutsford.size());
        Set<Route> knutsfordToAlty = routeRepository.findRoutesByName(agencyId,
                "Macclesfield - Altrincham");
        assertEquals(2, knutsfordToAlty.size());

        assertEquals(1, routeToRouteCosts.getFor(altyToKnutsford.iterator().next(), knutsfordToAlty.iterator().next(), date));

    }

}
