package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Route;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationWalk;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.RouteCostCalculator;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.resources.LocationJourneyPlanner;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.reference.KnownTramRoute;
import com.tramchester.testSupport.testTags.DataUpdateTest;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.TestEnv.nearAltrincham;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DataUpdateTest
class RouteCostCalculatorTest {

    private static ComponentContainer componentContainer;

    private RouteCostCalculator routeCostCalculator;
    private StationRepository stationRepository;
    private TramRouteHelper tramRouteHelper;
    private Transaction txn;
    private final TramServiceDate date = new TramServiceDate(TestEnv.testDay());
    private Station altrincham;
    private Station mediaCity;
    private Station airport;

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
        routeCostCalculator = componentContainer.get(RouteCostCalculator.class);
        stationRepository = componentContainer.get(StationRepository.class);
        tramRouteHelper = new TramRouteHelper(componentContainer);
        GraphDatabase database = componentContainer.get(GraphDatabase.class);
        txn = database.beginTx();

        altrincham = Altrincham.getFrom(stationRepository);
        mediaCity = MediaCityUK.getFrom(stationRepository);
        airport = ManAirport.getFrom(stationRepository);
    }

    @AfterEach
    void afterEachTestHasRun() {
        txn.close();
    }

    @Test
    void shouldComputeSimpleCostBetweenStationsAltyNavRoad() {
        assertEquals(3, routeCostCalculator.getAverageCostBetween(txn, NavigationRoad.getFrom(stationRepository), altrincham, date));
        assertEquals(4, routeCostCalculator.getAverageCostBetween(txn, altrincham, NavigationRoad.getFrom(stationRepository), date));
    }

    @Test
    void shouldComputeCostsForMediaCityAshton() {
        assertEquals(55, routeCostCalculator.getAverageCostBetween(txn, mediaCity, Ashton.getFrom(stationRepository), date));
        assertEquals(52, routeCostCalculator.getAverageCostBetween(txn,  Ashton.getFrom(stationRepository), mediaCity, date));
    }

    @Test
    void shouldComputeSimpleCostBetweenStationsAltyBury() {
        // changes regularly with timetable updates

        final Station bury = Bury.getFrom(stationRepository);
        final int buryToAlty = routeCostCalculator.getAverageCostBetween(txn, bury, altrincham, date);
        final int altyToBury = routeCostCalculator.getAverageCostBetween(txn, altrincham, bury, date);

        assertEquals(63, buryToAlty);
        assertEquals(63, altyToBury);
    }

    @Test
    void shouldTestWithWalkAtStart() {
        // nearAltrincham to Deansgate

        LocationJourneyPlanner locationJourneyPlanner = componentContainer.get(LocationJourneyPlanner.class);

        UUID uniqueId = UUID.randomUUID();
        Node walkStartNode = locationJourneyPlanner.createWalkingNode(txn, nearAltrincham, uniqueId);
        StationWalk stationWalk = new StationWalk(altrincham, 13);
        Relationship walkRelationship = locationJourneyPlanner.createWalkRelationship(txn, walkStartNode, stationWalk,
                TransportRelationshipTypes.WALKS_TO);

        int result = routeCostCalculator.getAverageCostBetween(txn, walkStartNode, Deansgate.getFrom(stationRepository), date);

        walkRelationship.delete();
        walkStartNode.delete();

        assertEquals(37,result);
    }

    @Test
    void shouldTestWithWalkAtEnd() {
        // Deansgate, nearAltrincham

        LocationJourneyPlanner locationJourneyPlanner = componentContainer.get(LocationJourneyPlanner.class);

        UUID uniqueId = UUID.randomUUID();
        Node walkEndNode = locationJourneyPlanner.createWalkingNode(txn, nearAltrincham, uniqueId);
        StationWalk stationWalk = new StationWalk(altrincham, 13);
        Relationship walkRelationship = locationJourneyPlanner.createWalkRelationship(txn, walkEndNode, stationWalk,
                TransportRelationshipTypes.WALKS_FROM);

        int result = routeCostCalculator.getAverageCostBetween(txn, Deansgate.getFrom(stationRepository), walkEndNode, date);

        walkRelationship.delete();
        walkEndNode.delete();

        assertEquals(37,result);

    }

    @Test
    void shouldComputeSimpleCostBetweenStationsMediaCityAirport() {
        assertEquals(58, routeCostCalculator.getAverageCostBetween(txn, mediaCity, airport, date));
        assertEquals(58, routeCostCalculator.getAverageCostBetween(txn, airport, mediaCity, date));
    }

    @Test
    void shouldComputeSimpleMaxCostBetweenStationsMediaCityAirport() {
        assertEquals(58, routeCostCalculator.getMaxCostBetween(txn, mediaCity, airport, date));
        assertEquals(58, routeCostCalculator.getMaxCostBetween(txn, airport, mediaCity, date));
    }

    @Test
    void shouldGetCostToInterchangeForRouteStation() {

        Set<Route> routes = tramRouteHelper.get(KnownTramRoute.AltrinchamPiccadilly);

        List<RouteStation> navigationRoadRouteStations = stationRepository.getRouteStationsFor(NavigationRoad.getId()).stream().
                filter(routeStation -> routes.contains(routeStation.getRoute())).collect(Collectors.toList());

        assertFalse(navigationRoadRouteStations.isEmpty());

        RouteStation navigationRoad = navigationRoadRouteStations.get(0);

        int cost = routeCostCalculator.costToInterchange(txn, navigationRoad);

        // cost to trafford bar
        assertEquals(14, cost);
    }

    @Test
    void shouldGetCostToInterchangeForRouteStationAdjacent() {

        Set<Route> routes = tramRouteHelper.get(KnownTramRoute.AltrinchamPiccadilly);

        List<RouteStation> oldTraffordRouteStations = stationRepository.getRouteStationsFor(OldTrafford.getId()).stream().
                filter(routeStation -> routes.contains(routeStation.getRoute())).collect(Collectors.toList());

        assertFalse(oldTraffordRouteStations.isEmpty());

        RouteStation oldTrafford = oldTraffordRouteStations.get(0);

        int cost = routeCostCalculator.costToInterchange(txn, oldTrafford);

        // cost to trafford bar
        assertEquals(2, cost);

    }

    @Test
    void shouldGetZeroCostToInterchangeForRouteStationThatIsInterchange() {

        Set<Route> routes = tramRouteHelper.get(KnownTramRoute.AltrinchamPiccadilly);

        List<RouteStation> cornbrookRouteStations = stationRepository.getRouteStationsFor(Cornbrook.getId()).stream().
                filter(routeStation -> routes.contains(routeStation.getRoute())).collect(Collectors.toList());

        assertFalse(cornbrookRouteStations.isEmpty());

        cornbrookRouteStations.forEach(cornbrookRoute -> {
                    int cost = routeCostCalculator.costToInterchange(txn, cornbrookRoute);
                    assertEquals(0, cost);
                }
        );
    }


}
