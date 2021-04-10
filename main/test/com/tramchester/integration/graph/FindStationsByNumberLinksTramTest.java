package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.FindStationsByNumberLinks;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FindStationsByNumberLinksTramTest {
    private static ComponentContainer componentContainer;
    private FindStationsByNumberLinks finder;

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
    void beforeEachTestRuns() {
        finder = componentContainer.get(FindStationsByNumberLinks.class);
    }

    @Test
    void shouldIdForkPointsFromTramNetwork() {
        int threshhold = 3;
        IdSet<Station> found = finder.findAtLeastNConnectionsFrom(TransportMode.Tram, threshhold);
        assertEquals(10, found.size());
        assertTrue(found.contains(TramStations.StPetersSquare.getId()));
        assertTrue(found.contains(TramStations.Broadway.getId()));
        assertTrue(found.contains(TramStations.PiccadillyGardens.getId()));
        assertTrue(found.contains(TramStations.MarketStreet.getId()));
        assertTrue(found.contains(TramStations.Pomona.getId()));
        assertTrue(found.contains(TramStations.TraffordBar.getId()));
        assertTrue(found.contains(TramStations.StWerburghsRoad.getId()));
        assertTrue(found.contains(TramStations.HarbourCity.getId()));
        assertTrue(found.contains(TramStations.Cornbrook.getId()));
        assertTrue(found.contains(TramStations.Victoria.getId()));

    }


}
