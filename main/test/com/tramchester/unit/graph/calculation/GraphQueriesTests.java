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
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tec.units.ri.quantity.Quantities;
import tec.units.ri.unit.Units;

import javax.measure.Quantity;
import javax.measure.quantity.Length;
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
        assertTrue(links.contains(createStationLink(modes, transportData.getFirst(), transportData.getSecond())));
        assertTrue(links.contains(createStationLink(modes, transportData.getSecond(), transportData.getInterchange())));
        assertTrue(links.contains(createStationLink(modes, transportData.getInterchange(), transportData.getFourthStation())));
        assertTrue(links.contains(createStationLink(modes, transportData.getInterchange(), transportData.getFifthStation())));
        assertTrue(links.contains(createStationLink(modes, transportData.getInterchange(), transportData.getLast())));
        assertTrue(links.contains(createStationLink(modes, transportData.getFirstDupName(), transportData.getFirstDup2Name())));

    }

    @NotNull
    private StationLink createStationLink(Set<TransportMode> modes, Station first, Station second) {
        // distance and time not used in equality
        Quantity<Length> distance = Quantities.getQuantity(11.1111D, Units.METRE);
        int walkingTimeMins = 42;
        return new StationLink(first, second, modes, distance, walkingTimeMins);
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
