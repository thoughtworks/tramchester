package com.tramchester.integration.repository.naptan;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.domain.Platform;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.NaptanArea;
import com.tramchester.domain.places.NaptanRecord;
import com.tramchester.domain.places.Station;
import com.tramchester.geo.GridPosition;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfigWithNaptan;
import com.tramchester.repository.naptan.NaptanRespository;
import com.tramchester.repository.naptan.NaptanStopType;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.BusStations;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.*;

import java.util.List;

import static com.tramchester.integration.testSupport.Assertions.assertIdEquals;
import static com.tramchester.testSupport.TestEnv.MANCHESTER_AIRPORT_BUS_AREA;
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

        NaptanRecord data = respository.getForActo(actoCode);
        assertEquals("Manchester City Centre", data.getSuburb());

        IdSet<NaptanArea> activeAreaCodes = respository.activeCodes(data.getAreaCodes());
        assertFalse(activeAreaCodes.isEmpty());
        //assertTrue(activeAreaCodes.contains(data.getAreaCodes()));

        IdSet<NaptanRecord> allRecordsForArea = activeAreaCodes.stream().
                flatMap(activeArea -> respository.getRecordsFor(activeArea).stream()).
                collect(IdSet.collector());

        assertEquals(3, allRecordsForArea.size(), allRecordsForArea.toString());
    }

    @Test
    void shouldContainBusStopWithinArea() {
        IdFor<Station> actoCode = BusStations.ManchesterAirportStation.getId();
        assertTrue(respository.containsActo(actoCode));

        NaptanRecord record = respository.getForActo(actoCode);
        assertEquals("Manchester Airport", record.getSuburb());

        final List<IdFor<NaptanArea>> areaCodes = record.getAreaCodes().toList();
        assertEquals(1, areaCodes.size());
        assertIdEquals(MANCHESTER_AIRPORT_BUS_AREA, areaCodes.get(0));
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

    @Disabled("WIP")
    @Test
    void shouldHaveExpectedStructureForMultiplatformTram() {

        IdFor<Station> stationId = TramStations.StPetersSquare.getId();

        assertTrue(respository.containsActo(stationId));
        NaptanRecord stationRecord = respository.getForActo(stationId);

        assertEquals(NaptanStopType.tramMetroUndergroundAccess, stationRecord.getStopType());

        IdSet<NaptanArea> stationAreaCodes = stationRecord.getAreaCodes();

        IdFor<Platform> platformId = StringIdFor.createId("9400ZZMASTP1");

//        assertTrue(respository.containsActo(platformId));
//        NaptanRecord platform1Record = respository.getForActo(platformId);
//
//        assertEquals(NaptanStopType.tramMetroUndergroundPlatform, platform1Record.getStopType());
//        assertEquals("Platform 1", platform1Record.getName());
//
//        IdSet<NaptanArea> platformAreaCodes = platform1Record.getAreaCodes();
//
//        IdSet<NaptanArea> overlaps = stationAreaCodes.intersection(platformAreaCodes);
//        assertFalse(overlaps.isEmpty());

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
    void shouldFindKnutsford() {

        final IdFor<Station> stopId = StringIdFor.createId("0600MA6020");
        NaptanRecord fromNaptan = respository.getForActo(stopId);
        assertNotNull(fromNaptan);

        // knutsford no longer has an area code in the data
//        IdSet<NaptanArea> areaCodes = fromNaptan.getAreaCodes();
//        assertFalse(areaCodes.isEmpty(), "no area codes " + fromNaptan);
//
//        IdSet<NaptanArea> activeAreaCodes = respository.activeCodes(areaCodes);
//        assertFalse(activeAreaCodes.isEmpty());
//        assertTrue(activeAreaCodes.contains(KnutfordStationAreaId));
//
//        IdSet<NaptanRecord> allRecordsForArea = activeAreaCodes.stream().
//                flatMap(activeArea -> respository.getRecordsFor(activeArea).stream()).
//                collect(IdSet.collector());
//
//        assertEquals(4, allRecordsForArea.size(), allRecordsForArea.toString());
    }

    @Test
    void shouldNotContainStopOutOfArea() {
        // stop in bristol, checked exists in full data in NaPTANDataImportTest
        IdFor<Station> actoCode = StringIdFor.createId(TestEnv.BRISTOL_BUSSTOP_OCTOCODE);
        assertFalse(respository.containsActo(actoCode));
    }
}
