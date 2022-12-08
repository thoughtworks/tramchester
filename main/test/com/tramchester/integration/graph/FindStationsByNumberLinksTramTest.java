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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static com.tramchester.testSupport.reference.TramStations.*;
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

    @Test
    void shouldIdInterchangePointsLinked() {

        IdSet<Station> found = finder.atLeastNLinkedStations(TransportMode.Tram, threshhold);

        List<IdFor<Station>> expectedList = Arrays.asList(StPetersSquare.getId(),
            MarketStreet.getId(),
            TraffordBar.getId(),
            Cornbrook.getId(),
            Victoria.getId(),
            StWerburghsRoad.getId(),
            Pomona.getId(),
            Broadway.getId(),
            HarbourCity.getId(),
            //Piccadilly.getId(),
            PiccadillyGardens.getId()
            );

        IdSet<Station> expected = new IdSet<>(expectedList);
        IdSet<Station> diff = IdSet.disjunction(found, expected);

        assertTrue(diff.isEmpty(), diff + " between expected:" + expected + " found:" + found);

    }

}
