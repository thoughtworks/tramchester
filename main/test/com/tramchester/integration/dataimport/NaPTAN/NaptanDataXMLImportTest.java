package com.tramchester.integration.dataimport.NaPTAN;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.dataimport.NaPTAN.NaptanStopsDataImporter;
import com.tramchester.dataimport.NaPTAN.xml.NaptanStopData;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfigWithXMLNaptan;
import com.tramchester.repository.naptan.NaptanStopType;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.*;

import java.util.Optional;
import java.util.stream.Stream;

import static com.tramchester.testSupport.reference.BusStations.BuryInterchange;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class NaptanDataXMLImportTest {

    private static GuiceContainerDependencies componentContainer;
    private Stream<NaptanStopData> dataStream;
    private NaptanStopsDataImporter dataImporter;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        IntegrationTramTestConfig testConfig = new IntegrationTramTestConfigWithXMLNaptan();
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
        dataStream.close();
        dataImporter.stop();
    }

    // was for initial diagnostics, likely changes too often
    @Test
    void shouldHaveLoadedSomeData() {
        long haveId = dataStream.count();
        assertEquals(439581, haveId);
    }

    @Test
    void shouldLoadKnownBusStation() {

        final String buryId = BuryInterchange.getId().forDTO();

        Optional<NaptanStopData> foundKnown = dataStream.
                filter(stop -> stop.getAtcoCode()!=null).
                filter(stop -> stop.getAtcoCode().equals(buryId)).
                findFirst();

        assertFalse(foundKnown.isEmpty());
    }

    @Test
    void shouldLoadKnownTramStation() {

        Optional<NaptanStopData> foundKnown = dataStream.
                filter(stop -> stop.getAtcoCode()!=null).
                filter(stop -> stop.getAtcoCode().equals(TramStations.StPetersSquare.getId().forDTO())).
                findFirst();
        assertFalse(foundKnown.isEmpty());

        NaptanStopData known = foundKnown.get();
        assertEquals(NaptanStopType.tramMetroUndergroundAccess, known.getStopType());
    }

    @Test
    void shouldContainOutofAreaStop() {
        Optional<NaptanStopData> foundKnown = dataStream.
                filter(stop -> stop.getAtcoCode()!=null).
                filter(stop -> stop.getAtcoCode().equals(TestEnv.BRISTOL_BUSSTOP_OCTOCODE)).
                findFirst();

        assertFalse(foundKnown.isEmpty());
        NaptanStopData known = foundKnown.get();
        assertEquals(NaptanStopType.busCoachTrolleyStopOnStreet, known.getStopType());
    }
}
