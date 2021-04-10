package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.StationLink;
import com.tramchester.graph.search.FindStationLinks;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
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
        findStationLinks = componentContainer.get(FindStationLinks.class);
    }

    @Test
    void shouldFindExpectedLinksBetweenStations() {
        Set<StationLink> results = findStationLinks.findFor(Tram);
        assertEquals(202, results.size());

        assertTrue(results.contains(createLink(StPetersSquare, PiccadillyGardens)));
        assertTrue(results.contains(createLink(StPetersSquare, MarketStreet)));
        assertTrue(results.contains(createLink(StPetersSquare, Deansgate)));

        assertTrue(results.contains(createLink(PiccadillyGardens, StPetersSquare)));
        assertTrue(results.contains(createLink(MarketStreet, StPetersSquare)));
        assertTrue(results.contains(createLink(Deansgate, StPetersSquare)));

        assertTrue(results.contains(createLink(MediaCityUK, HarbourCity)));
        assertTrue(results.contains(createLink(MediaCityUK, Broadway)));
        assertTrue(results.contains(createLink(HarbourCity, Broadway)));

        assertFalse(results.contains(createLink(StPetersSquare, Shudehill)));
        assertFalse(results.contains(createLink(Shudehill, StPetersSquare)));

    }

    private StationLink createLink(TramStations stPetersSquare, TramStations piccadillyGardens) {
        return new StationLink(TramStations.of(stPetersSquare), TramStations.of(piccadillyGardens),
                Collections.singleton(Tram));
    }

}
