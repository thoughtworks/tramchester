package com.tramchester.integration.dataimport.NaPTAN;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.dataimport.NaPTAN.NaptanStopsDataImporter;
import com.tramchester.dataimport.NaPTAN.NaptanStopData;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfigWithCSVNaptan;
import com.tramchester.repository.naptan.NaptanStopType;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.*;

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
        IntegrationTramTestConfig testConfig = new IntegrationTramTestConfigWithCSVNaptan();
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
        assertEquals("Bury", buryInterchange.getLocalityName());
        assertEquals("", buryInterchange.getParentLocalityName());
    }

    @Test
    void shouldLoadKnownTramStation() {

        Optional<NaptanStopData> foundKnown = dataStream.
                filter(stop -> stop.getAtcoCode().equals(TramStations.StPetersSquare.getId().forDTO())).
                findFirst();
        assertFalse(foundKnown.isEmpty());

        NaptanStopData known = foundKnown.get();
        assertEquals("Manchester City Centre", known.getLocalityName());
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
        assertEquals("Bristol City Centre", known.getLocalityName());
        //assertEquals("Bristol", known.getParentLocalityName());
    }
}
