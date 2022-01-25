package com.tramchester.integration.dataimport.NaPTAN;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.NaPTAN.NaptanStopsDataImporter;
import com.tramchester.dataimport.NaPTAN.xml.NaptanStopData;
import com.tramchester.integration.testSupport.GraphDBTestConfig;
import com.tramchester.integration.testSupport.IntegrationTestConfig;
import com.tramchester.integration.testSupport.naptan.NaptanRemoteDataSourceConfig;
import com.tramchester.repository.naptan.NaptanStopType;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.*;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.tramchester.testSupport.reference.BusStations.BuryInterchange;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;


class NaptanDataCSVImportTest {

    private static GuiceContainerDependencies componentContainer;
    private Stream<NaptanStopData> dataStream;
    private NaptanStopsDataImporter dataImporter;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        TramchesterConfig testConfig = new TestConfigWithCSVNaptan();
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void onceAfterAllTestsHaveRun() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        dataImporter = componentContainer.get(NaptanStopsDataImporter.class);
        dataImporter.start();
        dataStream = dataImporter.getStopsData();
    }

    @AfterEach
    void afterEachTestRuns() {
        dataImporter.stop();
    }

    @Test
    void shouldLoadKnownBusStation() {

        Optional<NaptanStopData> foundKnown = dataStream.
                filter(stop -> stop.getAtcoCode().equals(BuryInterchange.getId().forDTO())).
                findFirst();
        assertFalse(foundKnown.isEmpty());

        NaptanStopData buryInterchange = foundKnown.get();
        assertEquals("Bury", buryInterchange.getSuburb());
        assertEquals("", buryInterchange.getTown());
    }

    @Test
    void shouldLoadKnownTramStation() {

        Optional<NaptanStopData> foundKnown = dataStream.
                filter(stop -> stop.getAtcoCode().equals(TramStations.StPetersSquare.getId().forDTO())).
                findFirst();
        assertFalse(foundKnown.isEmpty());

        NaptanStopData known = foundKnown.get();
        assertEquals("Manchester City Centre", known.getSuburb());
        assertEquals(NaptanStopType.tramMetroUndergroundAccess, known.getStopType());
        //assertEquals("Manchester", known.getParentLocalityName());
    }

    @Test
    void shouldContainOutofAreaStop() {
        Optional<NaptanStopData> foundKnown = dataStream.
                filter(stop -> stop.getAtcoCode().equals(TestEnv.BRISTOL_BUSSTOP_OCTOCODE)).
                findFirst();

        assertFalse(foundKnown.isEmpty());
        NaptanStopData known = foundKnown.get();
        assertEquals("Bristol City Centre", known.getSuburb());
    }

    private static class TestConfigWithCSVNaptan extends IntegrationTestConfig {

        private final NaptanRemoteDataSourceConfig remoteNaptanCSVConfig;

        public TestConfigWithCSVNaptan() {
            super(new GraphDBTestConfig("",""));
            final Path naptanLocalDataPath = Path.of("data/naptan");
            remoteNaptanCSVConfig = new NaptanRemoteDataSourceConfig(naptanLocalDataPath, false);
        }

        @Override
        protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
            return Collections.emptyList();
        }

        @Override
        public List<RemoteDataSourceConfig> getRemoteDataSourceConfig() {
            return List.of(remoteNaptanCSVConfig);
        }
    }
}
