package com.tramchester.integration.geo;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.places.Station;
import com.tramchester.geo.MarginInMeters;
import com.tramchester.geo.StationLocations;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TestPostcodes;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.BusTest;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@BusTest
class BusStationsLocationsTest {
    private static ComponentContainer componentContainer;
    private static IntegrationBusTestConfig testConfig;

    private StationLocations stationLocations;
    private MarginInMeters inMeters;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        testConfig = new IntegrationBusTestConfig();
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        stationLocations = componentContainer.get(StationLocations.class);
        inMeters =  MarginInMeters.of(testConfig.getNearestStopForWalkingRangeKM());
    }

    @AfterAll
    static void afterEachTestRuns() {
        componentContainer.close();
    }

    //
    // these tests here to support tuning config parameters for num and distance of stations
    //

    @Test
    void shouldGetAllStationsCloseToPiccGardens() {
        List<Station> result = stationLocations.nearestStationsSorted(TestPostcodes.NearPiccadillyGardens.getLatLong(),
                500, inMeters);
        assertEquals(24, result.size());
    }

    @Test
    void shouldGetAllStationsCloseToCentralBury() {
        List<Station> result = stationLocations.nearestStationsSorted(TestPostcodes.CentralBury.getLatLong(),
                500, inMeters);
        assertEquals(11, result.size());
    }

    @Test
    void shouldGetAllStationsCloseToCentralAlty() {
        List<Station> result = stationLocations.nearestStationsSorted(TramStations.Altrincham.getLatLong(),
                500, inMeters);
        assertEquals(10, result.size());
    }
}
