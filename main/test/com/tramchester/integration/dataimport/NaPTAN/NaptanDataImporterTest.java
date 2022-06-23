package com.tramchester.integration.dataimport.NaPTAN;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.dataimport.NaPTAN.xml.NaptanDataCallbackImporter;
import com.tramchester.dataimport.NaPTAN.xml.NaptanFromXMLFile;
import com.tramchester.dataimport.NaPTAN.xml.stopArea.NaptanStopAreaData;
import com.tramchester.dataimport.NaPTAN.xml.stopPoint.NaptanStopData;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.NaptanRecord;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfigWithNaptan;
import com.tramchester.repository.naptan.NaptanStopType;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.tramchester.testSupport.reference.BusStations.BuryInterchange;
import static org.junit.jupiter.api.Assertions.*;

class NaptanDataImporterTest {

    private static GuiceContainerDependencies componentContainer;
    private static List<NaptanStopData> loadedStops;
    private static List<NaptanStopAreaData> loadedAreas;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        IntegrationTramTestConfig testConfig = new IntegrationTramTestConfigWithNaptan();
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        NaptanDataCallbackImporter dataImporter = componentContainer.get(NaptanDataCallbackImporter.class);

        loadedStops = new ArrayList<>();
        loadedAreas = new ArrayList<>();
        dataImporter.loadData(new NaptanFromXMLFile.NaptanXmlConsumer() {
            @Override
            public void process(NaptanStopAreaData element) {
                loadedAreas.add(element);
            }

            @Override
            public void process(NaptanStopData element) {
                loadedStops.add(element);
            }
        });
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

        IdFor<NaptanRecord> buryId = StringIdFor.convert(BuryInterchange.getId());

        Optional<NaptanStopData> foundKnown = loadedStops.stream().
                filter(stop -> stop.getAtcoCode().isValid()).
                filter(stop -> stop.getAtcoCode().equals(buryId)).
                findFirst();

        assertFalse(foundKnown.isEmpty());
    }

    @Test
    void shouldLoadKnownTramStation() {

        IdFor<NaptanRecord> id = StringIdFor.convert(TramStations.StPetersSquare.getId());

        Optional<NaptanStopData> foundKnown = loadedStops.stream().
                filter(stop -> stop.getAtcoCode().isValid()).
                filter(stop -> stop.getAtcoCode().equals(id)).
                findFirst();
        assertFalse(foundKnown.isEmpty());

        NaptanStopData known = foundKnown.get();
        assertEquals(NaptanStopType.tramMetroUndergroundAccess, known.getStopType());
    }

    @Test
    void shouldContainOutofAreaStop() {
        IdFor<NaptanRecord> id = StringIdFor.createId(TestEnv.BRISTOL_BUSSTOP_OCTOCODE);

        Optional<NaptanStopData> foundKnown = loadedStops.stream().
                filter(stop -> stop.getAtcoCode().isValid()).
                filter(stop -> stop.getAtcoCode().equals(id)).
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
