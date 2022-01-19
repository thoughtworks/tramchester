package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.FindStationsByNumberLinks;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.Interchanges;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class FindStationsByNumberLinksTramTest {
    private static ComponentContainer componentContainer;
    private static TramchesterConfig config;
    private FindStationsByNumberLinks finder;
    private int threshhold;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        config = new IntegrationTramTestConfig();

        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        threshhold = Interchanges.getLinkThreshhold(TransportMode.Tram);
        finder = componentContainer.get(FindStationsByNumberLinks.class);
    }

    @Test
    void shouldNotDuplciateWithConfig() {
        List<GTFSSourceConfig> dataSources = config.getGTFSDataSource();
        assertFalse(dataSources.isEmpty());

        GTFSSourceConfig dataSource = dataSources.get(0);
        assertEquals(DataSourceID.tfgm, dataSource.getDataSourceId());

        Set<String> fromConfig = dataSource.getAdditionalInterchanges();

        IdSet<Station> found = finder.atLeastNLinkedStations(TransportMode.Tram, threshhold);

        Set<String> discovered = found.stream().map(IdFor::forDTO).collect(Collectors.toSet());

        Set<String> dups = fromConfig.stream().filter(discovered::contains).collect(Collectors.toSet());

        assertTrue(dups.isEmpty(), "Found dups in config " + dups);
    }

    // TODO this is problematic for some datasets because they duplicate routes for different date ranges
    // so need to post filter the discovered routes to somehow spot they are not the same??
    @Test
    void shouldFindInterchangeRoutes() {
        IdSet<Station> found = finder.atLeastNLinkedRoutes(TransportMode.Tram, 9);
        assertEquals(5, found.size(), found.toString());
        assertTrue(found.contains(TramStations.TraffordBar.getId()));
        assertTrue(found.contains(TramStations.Cornbrook.getId()));
        assertTrue(found.contains(TramStations.Deansgate.getId()));
        assertTrue(found.contains(TramStations.StPetersSquare.getId()));
        assertTrue(found.contains(TramStations.Victoria.getId()));
    }

    @Test
    void shouldIdInterchangePointsLinked() {

        IdSet<Station> found = finder.atLeastNLinkedStations(TransportMode.Tram, threshhold);
        assertEquals(10, found.size(), found.toString());
        assertTrue(found.contains(TramStations.StPetersSquare.getId()));
        assertTrue(found.contains(TramStations.PiccadillyGardens.getId()));
        assertTrue(found.contains(TramStations.MarketStreet.getId()));
        assertTrue(found.contains(TramStations.TraffordBar.getId()));
        assertTrue(found.contains(TramStations.Cornbrook.getId()));
        assertTrue(found.contains(TramStations.Victoria.getId()));
        assertTrue(found.contains(TramStations.StWerburghsRoad.getId()));
        assertTrue(found.contains(TramStations.Pomona.getId()));

        assertTrue(found.contains(TramStations.Broadway.getId()));
        assertTrue(found.contains(TramStations.HarbourCity.getId()));

    }

}
