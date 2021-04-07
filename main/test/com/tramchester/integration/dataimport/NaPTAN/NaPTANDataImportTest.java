package com.tramchester.integration.dataimport.NaPTAN;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.dataimport.NaPTAN.NaPTANDataImporter;
import com.tramchester.dataimport.NaPTAN.StopsData;
import com.tramchester.integration.testSupport.IntegrationTramTestConfig;
import com.tramchester.repository.TransportDataFactory;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.*;

import java.util.Optional;
import java.util.stream.Stream;

import static com.tramchester.testSupport.reference.BusStations.BuryInterchange;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;


class NaPTANDataImportTest {

    private static GuiceContainerDependencies<TransportDataFactory> componentContainer;
    private Stream<StopsData> dataStream;
    private NaPTANDataImporter dataImporter;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        IntegrationTramTestConfig testConfig = new IntegrationTramTestConfig();
        componentContainer = new ComponentsBuilder<>().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void onceAfterAllTestsHaveRun() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        dataImporter = componentContainer.get(NaPTANDataImporter.class);
        dataImporter.start();
        dataStream = dataImporter.getAll();
    }

    @AfterEach
    void afterEachTestRuns() {
        dataImporter.stop();
    }

    @Test
    void shouldLoadKnownBusStation() {

        Optional<StopsData> foundKnown = dataStream.
                filter(stop -> stop.getAtcoCode().equals(BuryInterchange.getId().forDTO())).
                findFirst();
        assertFalse(foundKnown.isEmpty());

        StopsData buryInterchange = foundKnown.get();
        assertEquals("Bury", buryInterchange.getLocalityName());
        assertEquals("", buryInterchange.getParentLocalityName());
    }

    @Test
    void shouldLoadKnownTramStation() {

        Optional<StopsData> foundKnown = dataStream.
                filter(stop -> stop.getAtcoCode().equals(TramStations.StPetersSquare.getId().forDTO())).
                findFirst();
        assertFalse(foundKnown.isEmpty());

        StopsData known = foundKnown.get();
        assertEquals("Manchester City Centre", known.getLocalityName());
        assertEquals("Manchester", known.getParentLocalityName());
    }

    @Test
    void shouldContainOutofAreaStop() {
        Optional<StopsData> foundKnown = dataStream.
                filter(stop -> stop.getAtcoCode().equals(TestEnv.BRISTOL_BUSSTOP_OCTOCODE)).
                findFirst();

        assertFalse(foundKnown.isEmpty());
        StopsData known = foundKnown.get();
        assertEquals("Bristol City Centre", known.getLocalityName());
        assertEquals("Bristol", known.getParentLocalityName());
    }
}
