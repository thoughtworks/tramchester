package com.tramchester.integration.repository.naptan;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.NaptanArea;
import com.tramchester.domain.places.NaptanRecord;
import com.tramchester.domain.places.Station;
import com.tramchester.geo.GridPosition;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfigWithXMLNaptan;
import com.tramchester.repository.naptan.NaptanRespository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.BusStations;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.tramchester.integration.testSupport.Assertions.assertIdEquals;
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

        NaptanRecord record = respository.getForActo(actoCode);
        assertEquals("Manchester Airport", record.getSuburb());

        final List<IdFor<NaptanArea>> areaCodes = record.getAreaCodes().toList();
        assertEquals(1, areaCodes.size());
        assertIdEquals("180GMABS", areaCodes.get(0));
    }

    @Test
    void shouldNotContainTrainOutOfBounds() {
        assertFalse(respository.containsTiploc(RailStationIds.LondonEuston.getId()));
    }

    @Test
    void shouldHaveDataForTrainStation() {
        IdFor<Station> tiploc = RailStationIds.Macclesfield.getId();

        assertTrue(respository.containsTiploc(tiploc));

        NaptanRecord record = respository.getForTiploc(tiploc);
        assertEquals(record.getName(), "Macclesfield Rail Station");
        assertEquals(record.getSuburb(), "Macclesfield");
        assertEquals(record.getId(), StringIdFor.createId("9100MACLSFD"));

        final List<IdFor<NaptanArea>> areaCodes = record.getAreaCodes().toList();

        assertEquals(1, areaCodes.size());
        assertIdEquals("910GMACLSFD", areaCodes.get(0));
    }

    @Test
    void shouldHaveAltyTrainStation() {
        IdFor<Station> altyTrainId = RailStationIds.Altrincham.getId();

        assertTrue(respository.containsTiploc(altyTrainId));
        NaptanRecord record = respository.getForTiploc(altyTrainId);

        assertEquals("Altrincham Rail Station", record.getName());
        assertEquals("Altrincham", record.getSuburb());
        assertEquals(StringIdFor.createId("9100ALTRNHM"), record.getId());

        final List<IdFor<NaptanArea>> areaCodes = record.getAreaCodes().toList();
        assertEquals(1, areaCodes.size());
        assertIdEquals("910GALTRNHM", areaCodes.get(0));
    }

    @Test
    void shouldHaveNaptanAreaForAltrinchamStation() {
        final IdFor<NaptanArea> altyRailStationArea = StringIdFor.createId("910GALTRNHM");
        assertTrue(respository.containsArea(altyRailStationArea));
        NaptanArea area = respository.getAreaFor(altyRailStationArea);
        assertEquals("Altrincham Rail Station", area.getName());
        assertEquals(new GridPosition(377026, 387931), area.getGridPosition());
    }

    @Test
    void shouldNotContainAreaOutOfBounds() {
        assertFalse(respository.containsArea(StringIdFor.createId("910GEUSTON")));
    }

    @Test
    void shouldHaveAltyTramStation() {

        IdFor<Station> altyTramId = TramStations.Altrincham.getId();

        assertTrue(respository.containsActo(altyTramId));
        NaptanRecord record = respository.getForActo(altyTramId);

        assertEquals("Altrincham (Manchester Metrolink)", record.getName());
        assertEquals("Altrincham", record.getSuburb());

        final List<IdFor<NaptanArea>> areaCodes = record.getAreaCodes().toList();
        assertEquals(1, areaCodes.size());
        assertIdEquals("940GZZMAALT", areaCodes.get(0));
    }

    @Test
    void shouldNotContainStopOutOfArea() {
        // stop in bristol, checked exists in full data in NaPTANDataImportTest
        IdFor<Station> actoCode = StringIdFor.createId(TestEnv.BRISTOL_BUSSTOP_OCTOCODE);
        assertFalse(respository.containsActo(actoCode));
    }
}
