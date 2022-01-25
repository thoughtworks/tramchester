package com.tramchester.integration.dataimport.NaPTAN;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.dataimport.NaPTAN.NaptanStopsDataImporter;
import com.tramchester.dataimport.NaPTAN.xml.NaptanStopXMLData;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfigWithXMLNaptan;
import com.tramchester.repository.naptan.NaptanStopType;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.reference.BusStations.BuryInterchange;
import static org.junit.jupiter.api.Assertions.*;

class NaptanDataXMLImportTest {

    private static GuiceContainerDependencies componentContainer;
    private static List<NaptanStopXMLData> loadedStops;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        IntegrationTramTestConfig testConfig = new IntegrationTramTestConfigWithXMLNaptan();
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        NaptanStopsDataImporter dataImporter = componentContainer.get(NaptanStopsDataImporter.class);
        loadedStops = dataImporter.getStopsData().collect(Collectors.toList());
    }

    @AfterAll
    static void onceAfterAllTestsHaveRun() {
        loadedStops.clear();
        componentContainer.close();
    }

    // was for initial diagnostics, likely changes too often
    @Test
    void shouldHaveLoadedSomeData() {
        assertTrue(loadedStops.size() > 400000, "m");
    }

    @Test
    void shouldLoadKnownBusStation() {

        final String buryId = BuryInterchange.getId().forDTO();

        Optional<NaptanStopXMLData> foundKnown = loadedStops.stream().
                filter(stop -> stop.getAtcoCode()!=null).
                filter(stop -> stop.getAtcoCode().equals(buryId)).
                findFirst();

        assertFalse(foundKnown.isEmpty());
    }

    @Test
    void shouldLoadKnownTramStation() {

        Optional<NaptanStopXMLData> foundKnown = loadedStops.stream().
                filter(stop -> stop.getAtcoCode()!=null).
                filter(stop -> stop.getAtcoCode().equals(TramStations.StPetersSquare.getId().forDTO())).
                findFirst();
        assertFalse(foundKnown.isEmpty());

        NaptanStopXMLData known = foundKnown.get();
        assertEquals(NaptanStopType.tramMetroUndergroundAccess, known.getStopType());
    }

    @Test
    void shouldContainOutofAreaStop() {
        Optional<NaptanStopXMLData> foundKnown = loadedStops.stream().
                filter(stop -> stop.getAtcoCode()!=null).
                filter(stop -> stop.getAtcoCode().equals(TestEnv.BRISTOL_BUSSTOP_OCTOCODE)).
                findFirst();

        assertFalse(foundKnown.isEmpty());
        NaptanStopXMLData known = foundKnown.get();
        assertEquals(NaptanStopType.busCoachTrolleyStopOnStreet, known.getStopType());
    }
}
