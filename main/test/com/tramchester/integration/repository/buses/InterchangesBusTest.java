package com.tramchester.integration.repository.buses;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.InterchangeStation;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.RouteRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.BusTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.reference.BusStations.*;
import static org.junit.jupiter.api.Assertions.*;

@BusTest
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class InterchangesBusTest {
    private static ComponentContainer componentContainer;
    private InterchangeRepository interchangeRepository;
    private RouteRepository routeRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        componentContainer = new ComponentsBuilder().create(new IntegrationBusTestConfig(), TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        routeRepository = componentContainer.get(RouteRepository.class);
        interchangeRepository = componentContainer.get(InterchangeRepository.class);
    }

    @Test
    void shouldFindBusInterchanges() {
        IdSet<Station> interchanges = interchangeRepository.getAllInterchanges().stream().
                //filter(interchange -> interchange.getTransportModes().contains(Bus)).
                map(InterchangeStation::getStationId).
                collect(IdSet.idCollector());
        assertFalse(interchanges.isEmpty());

        assertFalse(interchanges.contains(StockportAtAldi.getId()));
        assertFalse(interchanges.contains(StockportNewbridgeLane.getId()));

        assertTrue(interchanges.contains(StopAtAltrinchamInterchange.getId()));
        // stockport bus station is closed
        //assertTrue(interchanges.contains(StopAtStockportBusStation.getId()));

    }

    @Test
    void shouldNotCountLinksForSameRoute() {
        Route route = routeRepository.getRouteById(StringIdFor.createId("SCMN149A:O:CURRENT"));
        Set<Station> stationsForRoute = route.getTrips().stream().
                flatMap(trip -> trip.getStopCalls().getStationSequence().stream()).
                collect(Collectors.toSet());

        long interchanges = stationsForRoute.stream().filter(station -> interchangeRepository.isInterchange(station)).count();

        assertEquals(10, interchanges, "too many interchanges for route with " + stationsForRoute.size() + " stations");
    }

    @Test
    void shouldAllBeSingleModeForBus() {
        Set<InterchangeStation> interchanges = interchangeRepository.getAllInterchanges();
        interchanges.forEach(interchangeStation -> assertFalse(interchangeStation.isMultiMode(), interchangeStation.toString()));
    }

    // TODO Reinstate
//    @Test
//    void shouldHaveReachableInterchangeForEveryRoute() {
//        Set<Route> routesWithInterchanges = RoutesWithInterchanges(interchangeRepository, Bus);
//        Set<Route> all = routeRepository.getRoutes();
//
//        // Note works for 2 links, not for 3 links
//        assertEquals(all, routesWithInterchanges);
//    }



}
