package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.FindStationsByNumberConnections;
import com.tramchester.integration.testSupport.IntegrationTramTestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FindStationsByNumberConnectionsTramTest {
    private static ComponentContainer componentContainer;
    private FindStationsByNumberConnections discoverer;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        IntegrationTramTestConfig config = new IntegrationTramTestConfig();

        componentContainer = new ComponentsBuilder<>().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        discoverer = componentContainer.get(FindStationsByNumberConnections.class);
    }

    @Test
    void shouldIdForkPointsFromTramNetwork() {
        int threshhold = 3;
        IdSet<Station> found = discoverer.findFor(TransportMode.Tram, threshhold, false);
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

    @Test
    void shouldFineEndsOfLines() {
        int threshhold = 1;
        IdSet<Station> found = discoverer.findFor(TransportMode.Tram, threshhold, true);

        IdSet<Station> expected = TramStations.EndOfTheLine.stream().map(TramStations::getId).collect(IdSet.idCollector());
        assertEquals(expected, found);
    }

    @Test
    void shouldGetZeroMatchForBuses() {
        int threshhold = 1;
        IdSet<Station> found = discoverer.findFor(TransportMode.Bus, threshhold, false);
        assertEquals(0, found.size());
    }

}
