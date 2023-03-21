package com.tramchester.unit.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.*;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.RailRouteId;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.NaptanArea;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.reference.TramTransportDataForTestFactory;
import com.tramchester.unit.graph.calculation.SimpleGraphConfig;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.tramchester.graph.GraphPropertyKey.DAY_OFFSET;
import static org.junit.jupiter.api.Assertions.*;

public class GraphPropsTest {

    private static ComponentContainer componentContainer;
    private static SimpleGraphConfig config;
    private Transaction txn;
    private Node node;

    @BeforeAll
    static void onceBeforeAllTestRuns() throws IOException {
        config = new SimpleGraphConfig("graphquerytests.db");
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
        GraphDatabase graphDatabase = componentContainer.get(GraphDatabase.class);
        txn = graphDatabase.beginTx();
        node = txn.createNode();
    }

    @AfterEach
    void afterEachTestRuns() {
        // no commit
        txn.close();
    }

    @Test
    void shouldBeAbleToSetRouteStationId() {
        Node node = txn.createNode();

        IdFor<Route> routeId = StringIdFor.createId("routeId", Route.class);
        IdFor<RouteStation> id = RouteStation.createId(TramStations.ExchangeSquare.getId(), routeId);

        GraphProps.setRouteStationProp(node, id);

        IdFor<RouteStation> result = GraphProps.getRouteStationIdFrom(node);

        assertEquals(id, result);
    }

    @Test
    void shouldBeAbleToSetRailRouteStationId() {
        Node node = txn.createNode();

        IdFor<Route> routeId = getRailRouteId();

        IdFor<RouteStation> id = RouteStation.createId(RailStationIds.Stockport.getId(), routeId);

        GraphProps.setRouteStationProp(node, id);

        IdFor<RouteStation> result = GraphProps.getRouteStationIdFrom(node);

        assertEquals(id, result);
    }


    @Test
    void shouldBeAbleToSetRoute() {

        Route route = TestEnv.getTramTestRoute();

        GraphProps.setProperty(node, route);

        IdFor<Route> result = GraphProps.getRouteIdFrom(node);

        assertEquals(route.getId(), result);
    }

    @Test
    void shouldBeAbleToSetRailRoute() {

        IdFor<Route> routeId = getRailRouteId();

        Route route = MutableRoute.getRoute(routeId, "routeCode", "routeName", TestEnv.MetAgency(), TransportMode.Tram);

        GraphProps.setProperty(node, route);

        IdFor<Route> result = GraphProps.getRouteIdFrom(node);

        assertEquals(route.getId(), result);
    }

    @Test
    void shouldSetTimeCorrectly() {
        TramTime time = TramTime.of(23,42);

        GraphProps.setTimeProp(node, time);

        TramTime result = GraphProps.getTime(node);

        assertEquals(time, result);
    }

    @Test
    void shouldSetTimeWithNextDayCorrectly() {
        TramTime time = TramTime.nextDay(9,53);

        GraphProps.setTimeProp(node, time);

        TramTime result = GraphProps.getTime(node);

        assertEquals(time, result);

        Boolean flag = (Boolean) node.getProperty(DAY_OFFSET.getText());
        assertNotNull(flag);
        assertTrue(flag);
    }

    @Test
    void shouldAddTransportModes() {

        GraphProps.addTransportMode(node, TransportMode.Train);

        Set<TransportMode> result = GraphProps.getTransportModes(node);
        assertEquals(1, result.size());
        assertTrue(result.contains(TransportMode.Train));

        GraphProps.addTransportMode(node, TransportMode.Bus);

        result = GraphProps.getTransportModes(node);
        assertEquals(2, result.size());
        assertTrue(result.contains(TransportMode.Train));
        assertTrue(result.contains(TransportMode.Bus));

    }

    @Test
    void shouldAddSingleTransportMode() {
        GraphProps.setProperty(node, TransportMode.Train);

        TransportMode result = GraphProps.getTransportMode(node);

        assertEquals(result, TransportMode.Train);
    }
    
    @Test
    void shouldSetPlatformId() {

        IdFor<NaptanArea> areaId = StringIdFor.createId("areaId", NaptanArea.class);
        Station station = TramStations.PiccadillyGardens.fakeWithPlatform("2", TestEnv.stPetersSquareLocation(),
                DataSourceID.tfgm, areaId);

        List<Platform> platforms = new ArrayList<>(station.getPlatforms());
        Platform platform = platforms.get(0);

        GraphProps.setProperty(node, station);
        GraphProps.setProperty(node, platform);

        IdFor<Platform> platformId = GraphProps.getPlatformIdFrom(node);

        assertEquals(platform.getId(), platformId);
    }

    @Test
    void shouldSetCost() {
        Duration duration = Duration.ofMinutes(42);

        GraphProps.setCostProp(node, duration);

        Duration result = GraphProps.getCost(node);

        assertEquals(duration, result);
    }

    @Test
    void shouldSetCostCeiling() {
        Duration duration = Duration.ofMinutes(42).plusSeconds(15);

        GraphProps.setCostProp(node, duration);

        Duration result = GraphProps.getCost(node);

        assertEquals(Duration.ofMinutes(43), result);
    }

    @Test
    void shouldSetCostRoundUp() {
        Duration duration = Duration.ofMinutes(42).plusSeconds(55);

        GraphProps.setCostProp(node, duration);

        Duration result = GraphProps.getCost(node);

        assertEquals(Duration.ofMinutes(43), result);
    }

    @NotNull
    private IdFor<Route> getRailRouteId() {
        IdFor<Station> begin = RailStationIds.Macclesfield.getId();
        IdFor<Station> end = RailStationIds.Wimbledon.getId();
        IdFor<Agency> agency = StringIdFor.createId("agencyId", Agency.class);

        return new RailRouteId(begin, end, agency, 1);
    }

}
