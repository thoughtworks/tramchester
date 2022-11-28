package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.StationLink;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.search.FindStationLinks;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.mappers.Geography;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

class FindStationLinksTest {

    private static ComponentContainer componentContainer;
    private FindStationLinks findStationLinks;
    private StationRepository stationRepository;
    private Geography geography;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        IntegrationTramTestConfig config = new IntegrationTramTestConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachOfTheTestsRun() {
        stationRepository = componentContainer.get(StationRepository.class);
        findStationLinks = componentContainer.get(FindStationLinks.class);
        geography = componentContainer.get(Geography.class);
    }

    @Test
    void shouldFindExpectedLinksBetweenStations() {
        Set<StationLink> results = findStationLinks.findLinkedFor(Tram);
        assertEquals(204, results.size());

        assertTrue(results.contains(createLink(StPetersSquare, PiccadillyGardens)));

        assertTrue(results.contains(createLink(StPetersSquare, MarketStreet)));
        assertTrue(results.contains(createLink(StPetersSquare, Deansgate)));

        assertTrue(results.contains(createLink(PiccadillyGardens, StPetersSquare)));
        assertTrue(results.contains(createLink(MarketStreet, StPetersSquare)));
        assertTrue(results.contains(createLink(Deansgate, StPetersSquare)));

        assertFalse(results.contains(createLink(StPetersSquare, Shudehill)));
        assertFalse(results.contains(createLink(Shudehill, StPetersSquare)));

        assertTrue(results.contains(createLink(MediaCityUK, HarbourCity)));
        assertTrue(results.contains(createLink(MediaCityUK, Broadway)));

    }

    @Test
    void shouldHaveCorrectTransportMode() {
        Set<StationLink> forTrams = findStationLinks.findLinkedFor(Tram);
        long notTram = forTrams.stream().filter(link -> !link.getLinkingModes().contains(Tram)).count();
        assertEquals(0, notTram);
    }

    private StationLink createLink(TramStations stationA, TramStations stationB) {
        final Set<TransportMode> singleton = Collections.singleton(Tram);
        return StationLink.create(stationA.from(stationRepository), stationB.from(stationRepository),
                singleton, geography);
    }

}
