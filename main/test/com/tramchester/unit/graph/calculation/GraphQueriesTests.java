package com.tramchester.unit.graph.calculation;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.StationLink;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.FindRouteEndPoints;
import com.tramchester.graph.search.FindStationLinks;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramTransportDataForTestFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphQueriesTests {

    private static ComponentContainer componentContainer;
    private static SimpleGraphConfig config;
    private TramTransportDataForTestFactory.TramTransportDataForTest transportData;
    private StationRepository stationRepository;

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
        stationRepository = componentContainer.get(StationRepository.class);
        transportData = (TramTransportDataForTestFactory.TramTransportDataForTest) componentContainer.get(TransportData.class);
    }

    @Test
    void shouldHaveCorrectLinksBetweenStations() {
        FindStationLinks findStationLinks = componentContainer.get(FindStationLinks.class);

        Set<StationLink> links = findStationLinks.findLinkedFor(Tram);

        assertEquals(5, links.size());

        Set<TransportMode> modes = Collections.singleton(Tram);
        assertTrue(links.contains(new StationLink(transportData.getFirst(), transportData.getSecond(), modes)));
        assertTrue(links.contains(new StationLink(transportData.getSecond(), transportData.getInterchange(), modes)));
        assertTrue(links.contains(new StationLink(transportData.getInterchange(), transportData.getFourthStation(), modes)));
        assertTrue(links.contains(new StationLink(transportData.getInterchange(), transportData.getFifthStation(), modes)));
        assertTrue(links.contains(new StationLink(transportData.getInterchange(), transportData.getLast(), modes)));
    }

    @Test
    void shouldFindBeginningOfRoutes() {
        FindRouteEndPoints findRouteEndPoints = componentContainer.get(FindRouteEndPoints.class);

        IdSet<RouteStation> results = findRouteEndPoints.searchForStarts(Tram);

        IdSet<Station> stationIds = results.stream().
                map(stationRepository::getRouteStationById).map(RouteStation::getStationId).
                collect(IdSet.idCollector());

        IdSet<Station> expectedStationIds = createSet(transportData.getFirst(), transportData.getInterchange());
        assertEquals(expectedStationIds, stationIds);
    }

    @Test
    void shouldFindEndsOfRoutes() {
        FindRouteEndPoints findRouteEndPoints = componentContainer.get(FindRouteEndPoints.class);

        IdSet<RouteStation> results = findRouteEndPoints.searchForEnds(Tram);

        IdSet<Station> stationIds = results.stream().
                map(stationRepository::getRouteStationById).map(RouteStation::getStationId).
                collect(IdSet.idCollector());

        IdSet<Station> expectedStationIds = createSet(transportData.getFifthStation(), transportData.getLast(),
                transportData.getFourthStation());
        assertEquals(expectedStationIds, stationIds);
    }

//    @Test
//    void shouldFindHourRelationships() {
//        HourNodeCache hourNodeCache = componentContainer.get(HourNodeCache.class);
//        GraphQuery graphQuery = componentContainer.get(GraphQuery.class);
//        GraphDatabase graphDatabase = componentContainer.get(GraphDatabase.class);
//
//        Set<Long> foundFor8am = hourNodeCache.getNodeIdsFor(8);
//        assertEquals(5, foundFor8am.size());
//
//        Set<Long> foundFor9am = hourNodeCache.getNodeIdsFor(9);
//        assertEquals(1, foundFor9am.size());
//
//        List<Integer> hours = IntStream.rangeClosed(0,23).boxed().collect(Collectors.toList());
//        hours.remove((Integer) 8);
//        hours.remove((Integer) 9);
//        hours.forEach(hour -> assertTrue(hourNodeCache.getNodeIdsFor(hour).isEmpty(), "unexpected " + hour));
//
//        Set<Integer> found = new HashSet<>();
//        Set<RouteStation> routeStations = transportData.getRouteStations();
//        try (Transaction txn = graphDatabase.beginTx()) {
//            routeStations.forEach(routeStation -> {
//                Node routeStationNode = graphQuery.getRouteStationNode(txn, routeStation);
//                if (routeStationNode.hasRelationship(Direction.OUTGOING, TO_SERVICE)) {
//                    Relationship toService = routeStationNode.getSingleRelationship(TO_SERVICE, Direction.OUTGOING);
//                    Node serviceNode = toService.getEndNode();
//                    Iterable<Relationship> hourRelationships = serviceNode.getRelationships(Direction.OUTGOING, TO_HOUR);
//                    hourRelationships.forEach(hourRelationship -> {
//                        int result = hourNodeCache.getHourFor(hourRelationship.getEndNodeId());
//                        found.add(result);
//                    });
//                }
//            });
//        }
//        assertEquals(2, found.size());
//        assertTrue(found.contains(8));
//        assertTrue(found.contains(9));
//    }

    IdSet<Station> createSet(Station...stations) {
        return Arrays.stream(stations).map(Station::getId).collect(IdSet.idCollector());
    }
}
