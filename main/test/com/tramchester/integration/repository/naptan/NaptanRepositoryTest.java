package com.tramchester.integration.repository.naptan;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.dataimport.NaPTAN.xml.NaptanStopData;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfigWithCSVNaptan;
import com.tramchester.repository.naptan.NaptanRespository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.BusStations;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class NaptanRepositoryTest {
    private static GuiceContainerDependencies componentContainer;
    private NaptanRespository respository;

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
        respository = componentContainer.get(NaptanRespository.class);
    }

    @Test
    void shouldContainTramWithinArea() {
        IdFor<Station> actoCode = TramStations.Shudehill.getId();
        assertTrue(respository.containsActo(actoCode));

        NaptanStopData data = respository.getForActo(actoCode);
        assertEquals("Manchester City Centre", data.getSuburb());
    }

    @Test
    void shouldContainBusStopWithinArea() {
        IdFor<Station> actoCode = BusStations.ManchesterAirportStation.getId();
        assertTrue(respository.containsActo(actoCode));

        NaptanStopData data = respository.getForActo(actoCode);
        assertEquals("Manchester Airport", data.getSuburb());
    }

    @Disabled("wip to reinstate train data from the XML feed")
    @Test
    void shouldHaveDataForTrains() {
        IdFor<Station> tiploc = RailStationIds.Macclesfield.getId();

        assertTrue(respository.containsTiploc(tiploc));
        NaptanStopData data = respository.getForTiploc(tiploc);
        assertEquals(data.getCommonName(), "Macclesfield Rail Station");
        assertEquals(data.getSuburb(), "Macclesfield");
        assertEquals(data.getAtcoCode(), "9100MACLSFD");
    }

    @Test
    void shouldNotContainStopOutOfArea() {
        // stop in bristol, checked exists in full data in NaPTANDataImportTest
        IdFor<Station> actoCode = StringIdFor.createId(TestEnv.BRISTOL_BUSSTOP_OCTOCODE);
        assertFalse(respository.containsActo(actoCode));
    }
}
