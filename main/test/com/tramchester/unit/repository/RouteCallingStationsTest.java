package com.tramchester.unit.repository;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.repository.RouteCallingStations;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramTransportDataForTestFactory;
import com.tramchester.unit.graph.calculation.SimpleCompositeGraphConfig;
import com.tramchester.unit.graph.calculation.SimpleGraphConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


/// Helps with consistency of test data as well
public class RouteCallingStationsTest {
    private static ComponentContainer componentContainer;
    private static SimpleGraphConfig config;

    private RouteCallingStations routeCallingStations;
    private TramTransportDataForTestFactory.TramTransportDataForTest transportData;

    @BeforeAll
    static void onceBeforeAllTestRuns() throws IOException {
        config = new SimpleCompositeGraphConfig("tramroutetest.db");
        TestEnv.deleteDBIfPresent(config);

        componentContainer = new ComponentsBuilder().
                overrideProvider(TramTransportDataForTestFactory.class).
                create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void onceAfterAllTestsRun() throws IOException {
        TestEnv.clearDataCache(componentContainer);
        componentContainer.close();
        TestEnv.deleteDBIfPresent(config);
    }

    @BeforeEach
    void beforeEachTestRuns() {
        transportData = (TramTransportDataForTestFactory.TramTransportDataForTest) componentContainer.get(TransportData.class);
        routeCallingStations = componentContainer.get(RouteCallingStations.class);
    }

    @Test
    void shouldGetRouteACallingStations() {
        final Route routeA = transportData.getRouteA();
        IdSet<Station> stationIds = getCallingStationIdsFor(routeA);

        // 1st, 2nd, i/c, last
        assertEquals(4, stationIds.size(), stationIds.size());

        assertTrue(stationIds.contains(transportData.getFirst().getId()));
        assertTrue(stationIds.contains(transportData.getSecond().getId()));
        assertTrue(stationIds.contains(transportData.getInterchange().getId()));
        assertTrue(stationIds.contains(transportData.getLast().getId()));
    }

    @Test
    void shouldGetRouteBCallingStations() {
        final Route route = transportData.getRouteB();
        IdSet<Station> stationIds = getCallingStationIdsFor(route);

        assertEquals(1, stationIds.size(), stationIds.size());
        assertTrue(stationIds.contains(transportData.getInterchange().getId()));

    }

    @Test
    void shouldGetRouteCCallingStations() {
        final Route route = transportData.getRouteC();
        IdSet<Station> stationIds = getCallingStationIdsFor(route);

        assertEquals(1, stationIds.size(), stationIds.size());
        assertTrue(stationIds.contains(transportData.getInterchange().getId()));
    }

    @Test
    void shouldGetRouteDCallingStations() {
        final Route route = transportData.getRouteD();
        IdSet<Station> stationIds = getCallingStationIdsFor(route);

        assertEquals(2, stationIds.size(), stationIds.toString());

        assertTrue(stationIds.contains(transportData.getFirstDupName().getId()));
        assertTrue(stationIds.contains(transportData.getFirstDup2Name().getId()));

    }

    private IdSet<Station> getCallingStationIdsFor(Route route) {
        List<RouteCallingStations.StationWithCost> routeACallingPoints = routeCallingStations.getStationsFor(route);
        return routeACallingPoints.stream().map(RouteCallingStations.StationWithCost::getId).collect(IdSet.idCollector());
    }
}
