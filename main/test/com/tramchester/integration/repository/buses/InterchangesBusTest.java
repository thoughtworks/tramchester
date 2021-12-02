package com.tramchester.integration.repository.buses;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.InterchangeStation;
import com.tramchester.domain.id.IdSet;
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

import static com.tramchester.testSupport.reference.BusStations.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
