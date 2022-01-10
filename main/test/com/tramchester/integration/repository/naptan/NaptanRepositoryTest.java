package com.tramchester.integration.repository.naptan;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.dataimport.NaPTAN.StopsData;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfigWithNaptan;
import com.tramchester.repository.naptan.NaptanRespository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.BusStations;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NaptanRepositoryTest {
    private static GuiceContainerDependencies componentContainer;
    private NaptanRespository respository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        IntegrationTramTestConfig testConfig = new IntegrationTramTestConfigWithNaptan();
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

        StopsData data = respository.getForActo(actoCode);
        assertEquals("Manchester City Centre", data.getLocalityName());
    }

    @Test
    void shouldContainBusStopWithinArea() {
        IdFor<Station> actoCode = BusStations.ManchesterAirportStation.getId();
        assertTrue(respository.containsActo(actoCode));

        StopsData data = respository.getForActo(actoCode);
        assertEquals("Manchester Airport", data.getLocalityName());
    }

    @Test
    void shouldHaveDataForTrains() {
        IdFor<Station> tiploc = RailStationIds.Macclesfield.getId();

        assertTrue(respository.containsTiploc(tiploc));
        StopsData data = respository.getForTiploc(tiploc);
        assertEquals(data.getCommonName(), "Macclesfield Rail Station");
        assertEquals(data.getLocalityName(), "Macclesfield");
        assertEquals(data.getAtcoCode(), "9100MACLSFD");
    }

    @Test
    void shouldNotContainStopOutOfArea() {
        // stop in bristol, checked exists in full data in NaPTANDataImportTest
        IdFor<Station> actoCode = StringIdFor.createId(TestEnv.BRISTOL_BUSSTOP_OCTOCODE);
        assertFalse(respository.containsActo(actoCode));
    }
}
