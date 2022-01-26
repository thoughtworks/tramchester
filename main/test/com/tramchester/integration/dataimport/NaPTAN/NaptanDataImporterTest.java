package com.tramchester.integration.dataimport.NaPTAN;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.dataimport.NaPTAN.NaptanDataImporter;
import com.tramchester.dataimport.NaPTAN.xml.stopArea.NaptanStopAreaData;
import com.tramchester.dataimport.NaPTAN.xml.stopPoint.NaptanStopData;
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

class NaptanDataImporterTest {

    private static GuiceContainerDependencies componentContainer;
    private static List<NaptanStopData> loadedStops;
    private static List<NaptanStopAreaData> loadedAreas;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        IntegrationTramTestConfig testConfig = new IntegrationTramTestConfigWithXMLNaptan();
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        NaptanDataImporter dataImporter = componentContainer.get(NaptanDataImporter.class);
        loadedStops = dataImporter.getStopsData().collect(Collectors.toList());
        loadedAreas = dataImporter.getAreasData().collect(Collectors.toList());
    }

    @AfterAll
    static void onceAfterAllTestsHaveRun() {
        loadedStops.clear();
        loadedAreas.clear();
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

        Optional<NaptanStopData> foundKnown = loadedStops.stream().
                filter(stop -> stop.getAtcoCode()!=null).
                filter(stop -> stop.getAtcoCode().equals(buryId)).
                findFirst();

        assertFalse(foundKnown.isEmpty());
    }

    @Test
    void shouldLoadKnownTramStation() {

        Optional<NaptanStopData> foundKnown = loadedStops.stream().
                filter(stop -> stop.getAtcoCode()!=null).
                filter(stop -> stop.getAtcoCode().equals(TramStations.StPetersSquare.getId().forDTO())).
                findFirst();
        assertFalse(foundKnown.isEmpty());

        NaptanStopData known = foundKnown.get();
        assertEquals(NaptanStopType.tramMetroUndergroundAccess, known.getStopType());
    }

    @Test
    void shouldContainOutofAreaStop() {
        Optional<NaptanStopData> foundKnown = loadedStops.stream().
                filter(stop -> stop.getAtcoCode()!=null).
                filter(stop -> stop.getAtcoCode().equals(TestEnv.BRISTOL_BUSSTOP_OCTOCODE)).
                findFirst();

        assertFalse(foundKnown.isEmpty());
        NaptanStopData known = foundKnown.get();
        assertEquals(NaptanStopType.busCoachTrolleyStopOnStreet, known.getStopType());
    }

    @Test
    void shouldContainExpectedArea() {
        Optional<NaptanStopAreaData> foundKnown = loadedAreas.stream().
                filter(area -> area.getStopAreaCode()!=null).
                filter(area -> area.getStopAreaCode().equals("940GZZMAALT")).
                findFirst();

        assertFalse(foundKnown.isEmpty());
        NaptanStopAreaData known = foundKnown.get();
        assertEquals("Altrincham (Manchester Metrolink)", known.getName());
    }
}
