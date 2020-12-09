package com.tramchester.integration.geo;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.places.Station;
import com.tramchester.geo.StationLocations;
import com.tramchester.integration.testSupport.IntegrationBusTestConfig;
import com.tramchester.testSupport.reference.Postcodes;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class BusStationsLocationsTest {
    private static ComponentContainer componentContainer;
    private static IntegrationBusTestConfig testConfig;

    private StationLocations stationLocations;
    private Double nearestStopRangeKM;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        testConfig = new IntegrationBusTestConfig();
        componentContainer = new ComponentsBuilder().create(testConfig);
        componentContainer.initialise();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        stationLocations = componentContainer.get(StationLocations.class);
        nearestStopRangeKM = testConfig.getNearestStopForWalkingRangeKM();
    }

    @AfterAll
    static void afterEachTestRuns() {
        componentContainer.close();
    }

    //
    // to support tuning config parameters for num and distance of stations
    //

    @Test
    void shouldGetAllStationsCloseToPiccGardens() {
        List<Station> result = stationLocations.getNearestStationsTo(Postcodes.NearPiccadillyGardens.getLatLong(),
                500, nearestStopRangeKM);
        assertEquals(50, result.size());
    }

    @Test
    void shouldGetAllStationsCloseToCentralBury() {
        List<Station> result = stationLocations.getNearestStationsTo(Postcodes.CentralBury.getLatLong(),
                500, nearestStopRangeKM);
        assertEquals(19, result.size());
    }

    @Test
    void shouldGetAllStationsCloseToCentralAlty() {
        List<Station> result = stationLocations.getNearestStationsTo(TramStations.Altrincham.getLatLong(),
                500, nearestStopRangeKM);
        assertEquals(9, result.size());
    }
}
