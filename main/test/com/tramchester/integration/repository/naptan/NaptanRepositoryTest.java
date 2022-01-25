package com.tramchester.integration.repository.naptan;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.NaptanRecord;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfigWithXMLNaptan;
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
        respository = componentContainer.get(NaptanRespository.class);
    }

    @Test
    void shouldContainTramWithinArea() {
        IdFor<Station> actoCode = TramStations.Shudehill.getId();
        assertTrue(respository.containsActo(actoCode));

        NaptanRecord data = respository.getForActo(actoCode);
        assertEquals("Manchester City Centre", data.getSuburb());
    }

    @Test
    void shouldContainBusStopWithinArea() {
        IdFor<Station> actoCode = BusStations.ManchesterAirportStation.getId();
        assertTrue(respository.containsActo(actoCode));

        NaptanRecord data = respository.getForActo(actoCode);
        assertEquals("Manchester Airport", data.getSuburb());
    }

    @Test
    void shouldNotContainTrainOutOfBounds() {
        assertFalse(respository.containsTiploc(RailStationIds.LondonEuston.getId()));
    }

    @Disabled("awaiting rail data import from xml version of naptan feed")
    @Test
    void shouldHaveDataForTrains() {
        IdFor<Station> tiploc = RailStationIds.Macclesfield.getId();

        assertTrue(respository.containsTiploc(tiploc));
        NaptanRecord data = respository.getForTiploc(tiploc);
        assertEquals(data.getName(), "Macclesfield Rail Station");
        assertEquals(data.getSuburb(), "Macclesfield");
        assertEquals(data.getId(), StringIdFor.createId("9100MACLSFD"));
    }

    @Test
    void shouldNotContainStopOutOfArea() {
        // stop in bristol, checked exists in full data in NaPTANDataImportTest
        IdFor<Station> actoCode = StringIdFor.createId(TestEnv.BRISTOL_BUSSTOP_OCTOCODE);
        assertFalse(respository.containsActo(actoCode));
    }
}
