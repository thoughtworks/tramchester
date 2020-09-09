package com.tramchester.integration.geo;

import com.tramchester.Dependencies;
import com.tramchester.domain.places.Station;
import com.tramchester.geo.StationLocations;
import com.tramchester.integration.IntegrationBusTestConfig;
import com.tramchester.testSupport.Postcodes;
import com.tramchester.testSupport.TramStations;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class BusStationsLocationsTest {
    private static Dependencies dependencies;
    private static IntegrationBusTestConfig testConfig;

    private StationLocations stationLocations;
    private Double nearestStopRangeKM;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        dependencies = new Dependencies();
        testConfig = new IntegrationBusTestConfig();
        dependencies.initialise(testConfig);
    }

    @BeforeEach
    void beforeEachTestRuns() {
        stationLocations = dependencies.get(StationLocations.class);
        nearestStopRangeKM = testConfig.getNearestStopForWalkingRangeKM();
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
        assertEquals(18, result.size());
    }

    @Test
    void shouldGetAllStationsCloseToCentralAlty() {
        List<Station> result = stationLocations.getNearestStationsTo(TramStations.Altrincham.getLatLong(),
                500, nearestStopRangeKM);
        assertEquals(9, result.size());
    }
}
