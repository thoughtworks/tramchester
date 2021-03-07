package com.tramchester.unit.graph.calculation;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.StationLink;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.*;
import com.tramchester.graph.search.FindStationLinks;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramTransportDataForTestProvider;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.graph.TransportRelationshipTypes.TO_HOUR;
import static com.tramchester.graph.TransportRelationshipTypes.TO_SERVICE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphQueriesTests {

    private static ComponentContainer componentContainer;
    private static SimpleGraphConfig config;
    private TramTransportDataForTestProvider.TestTransportData transportData;
    private StationRepository stationRepository;

    @BeforeAll
    static void onceBeforeAllTestRuns() throws IOException {
        config = new SimpleGraphConfig("graphquerytests.db");

        FileUtils.deleteDirectory(config.getDBPath().toFile());

        componentContainer = new ComponentsBuilder<TramTransportDataForTestProvider>().
                overrideProvider(TramTransportDataForTestProvider.class).
                create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initContainer();
    }

    @AfterAll
    static void onceAfterAllTestsRun() throws IOException {
        componentContainer.close();
        FileUtils.deleteDirectory(config.getDBPath().toFile());
    }

    @BeforeEach
    void beforeEachTestRuns() {
        stationRepository = componentContainer.get(StationRepository.class);
        transportData = (TramTransportDataForTestProvider.TestTransportData) componentContainer.get(TransportData.class);
    }

    @Test
    void shouldHaveCorrectLinksBetweenStations() {
        FindStationLinks findStationLinks = componentContainer.get(FindStationLinks.class);

        Set<StationLink> links = findStationLinks.findFor(Tram);

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

    @Test
    void shouldFindHourRelationships() {
        HourNodeCache hourNodeCache = componentContainer.get(HourNodeCache.class);
        GraphQuery graphQuery = componentContainer.get(GraphQuery.class);
        GraphDatabase graphDatabase = componentContainer.get(GraphDatabase.class);

        Set<Long> foundFor8am = hourNodeCache.getRelationshipsFor(8);
        assertEquals(5, foundFor8am.size());

        Set<Long> foundFor9am = hourNodeCache.getRelationshipsFor(9);
        assertEquals(1, foundFor9am.size());

        List<Integer> hours = IntStream.rangeClosed(0,23).boxed().collect(Collectors.toList());
        hours.remove((Integer) 8);
        hours.remove((Integer) 9);
        hours.forEach(hour -> assertTrue(hourNodeCache.getRelationshipsFor(hour).isEmpty(), "unexpected " + hour));

        Set<Integer> found = new HashSet<>();
        Set<RouteStation> routeStations = transportData.getRouteStations();
        try (Transaction txn = graphDatabase.beginTx()) {
            routeStations.forEach(routeStation -> {
                Node routeStationNode = graphQuery.getRouteStationNode(txn, routeStation);
                if (routeStationNode.hasRelationship(Direction.OUTGOING, TO_SERVICE)) {
                    Relationship toService = routeStationNode.getSingleRelationship(TO_SERVICE, Direction.OUTGOING);
                    Node serviceNode = toService.getEndNode();
                    Iterable<Relationship> hourRelationships = serviceNode.getRelationships(Direction.OUTGOING, TO_HOUR);
                    hourRelationships.forEach(hourRelationship -> {
                        int result = hourNodeCache.getHourFor(hourRelationship.getEndNodeId());
                        found.add(result);
                    });
                }
            });
        }
        assertEquals(2, found.size());
        assertTrue(found.contains(8));
        assertTrue(found.contains(9));
    }

    IdSet<Station> createSet(Station...stations) {
        return Arrays.stream(stations).map(Station::getId).collect(IdSet.idCollector());
    }
}
