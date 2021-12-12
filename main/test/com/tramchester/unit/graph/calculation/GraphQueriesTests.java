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

        assertEquals(6, links.size());

        Set<TransportMode> modes = Collections.singleton(Tram);
        assertTrue(links.contains(new StationLink(transportData.getFirst(), transportData.getSecond(), modes)));
        assertTrue(links.contains(new StationLink(transportData.getSecond(), transportData.getInterchange(), modes)));
        assertTrue(links.contains(new StationLink(transportData.getInterchange(), transportData.getFourthStation(), modes)));
        assertTrue(links.contains(new StationLink(transportData.getInterchange(), transportData.getFifthStation(), modes)));
        assertTrue(links.contains(new StationLink(transportData.getInterchange(), transportData.getLast(), modes)));
        assertTrue(links.contains(new StationLink(transportData.getFirstDupName(), transportData.getFirstDup2Name(), modes)));

    }

    @Test
    void shouldFindBeginningOfRoutes() {
        FindRouteEndPoints findRouteEndPoints = componentContainer.get(FindRouteEndPoints.class);

        IdSet<RouteStation> results = findRouteEndPoints.searchForStarts(Tram);

        IdSet<Station> stationIds = results.stream().
                map(stationRepository::getRouteStationById).map(RouteStation::getStationId).
                collect(IdSet.idCollector());

        IdSet<Station> expectedStationIds = createSet(transportData.getFirst(), transportData.getInterchange(),
                transportData.getFirstDupName());
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
                transportData.getFourthStation(), transportData.getFirstDup2Name());
        assertEquals(expectedStationIds, stationIds);
    }

    IdSet<Station> createSet(Station...stations) {
        return Arrays.stream(stations).map(Station::getId).collect(IdSet.idCollector());
    }
}
