package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.FindStationsByNumberLinks;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.Interchanges;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.PiccGardens2022;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertEquals(1, dataSources.size());

        GTFSSourceConfig dataSource = dataSources.get(0);
        assertEquals(DataSourceID.tfgm, dataSource.getDataSourceId());

        IdSet<Station> stationWithLinks = finder.atLeastNLinkedStations(TransportMode.Tram, threshhold);

        IdSet<Station> inConfigAndStationsWithLinks = dataSource.getAdditionalInterchanges().stream().
                filter(stationWithLinks::contains).
                collect(IdSet.idCollector());

        assertTrue(inConfigAndStationsWithLinks.isEmpty(), "Found also in config " + inConfigAndStationsWithLinks +
                " stations with links were " + stationWithLinks);
    }

    @PiccGardens2022
    @Test
    void shouldIdInterchangePointsLinked() {

        IdSet<Station> found = finder.atLeastNLinkedStations(TransportMode.Tram, threshhold);
        // -1
        assertEquals(11-1, found.size(), found.toString());
        assertTrue(found.contains(TramStations.StPetersSquare.getId()));

//        assertTrue(found.contains(TramStations.PiccadillyGardens.getId()));
        assertTrue(found.contains(TramStations.MarketStreet.getId()));
        assertTrue(found.contains(TramStations.TraffordBar.getId()));
        assertTrue(found.contains(TramStations.Cornbrook.getId()));
        assertTrue(found.contains(TramStations.Victoria.getId()));
        assertTrue(found.contains(TramStations.StWerburghsRoad.getId()));
        assertTrue(found.contains(TramStations.Pomona.getId()));

        // not during eccles line works
        assertTrue(found.contains(TramStations.Broadway.getId()));

        assertTrue(found.contains(TramStations.HarbourCity.getId()));

    }

}
