package com.tramchester.integration.graph.buses;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Agency;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.NumberOfChanges;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.search.routes.RouteToRouteCosts;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.StationGroupsRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.BusStations;
import com.tramchester.testSupport.reference.BusStations.Composites;
import com.tramchester.testSupport.testTags.BusTest;
import org.junit.jupiter.api.*;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;


@BusTest
public class BusRouteToRouteCostsTest {

    private static ComponentContainer componentContainer;

    private RouteToRouteCosts routeToRouteCosts;
    private RouteRepository routeRepository;
    private StationGroupsRepository stationGroupsRepository;
    private StationRepository stationRepository;
    private TramDate date;
    private TimeRange timeRange;
    private Set<TransportMode> modes;

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
        timeRange = TimeRange.of(TramTime.of(04,45), TramTime.of(23,55));
        modes = TransportMode.BusesOnly;
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
        assertEquals(1, routeToRouteCosts.getNumberOfChanges(start, end, date, timeRange, modes).getMin());
    }

    @Test
    void shouldGetNumberOfRouteHopsBetweenAltrinchamShudehill() {
        StationGroup start = stationGroupsRepository.findByName(Composites.AltrinchamInterchange.getName());
        StationGroup end = stationGroupsRepository.findByName("Shudehill Interchange");

        NumberOfChanges numberOfChanges = routeToRouteCosts.getNumberOfChanges(start, end, date, timeRange, modes);
        assertEquals(1, numberOfChanges.getMin());
        assertEquals(3, numberOfChanges.getMax());
    }

    @Test
    void shouldGetNumberOfRouteHopsBetweenKnutsfordAndShudehill() {
        Station start = stationRepository.getStationById(BusStations.KnutsfordStationStand3.getId());
        StationGroup end = stationGroupsRepository.findByName("Shudehill Interchange");

        NumberOfChanges numberOfChanges = routeToRouteCosts.getNumberOfChanges(LocationSet.singleton(start),
                LocationSet.of(end.getContained()), date, timeRange, modes);

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

        NumberOfChanges numberOfChanges = routeToRouteCosts.getNumberOfChanges(altyToKnutsford.iterator().next(),
                knutsfordToAlty.iterator().next(), date, timeRange);
        assertEquals(1, numberOfChanges.getMin());

    }

}
