package com.tramchester.integration.dataimport;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.BusStations;
import com.tramchester.testSupport.testTags.BusTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@BusTest
class ValidateBusTestStations {

    private static ComponentContainer componentContainer;

    private StationRepository stationRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        componentContainer = new ComponentsBuilder().create(new IntegrationBusTestConfig(), TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        stationRepository = componentContainer.get(StationRepository.class);
    }

    @Test
    void shouldHaveCorrectTestBusStations() {
        List<BusStations> testStations = Arrays.asList(BusStations.values());

        testStations.forEach(enumValue -> {
            Station testStation = BusStations.of(enumValue);

            Station realStation = stationRepository.getStationById(testStation.getId());

            String testStationName = testStation.getName();
            assertEquals(realStation.getName(), testStationName, "name wrong for id: " + testStation.getId());
            //assertEquals(realStation.getArea(), testStation.getArea(),"area wrong for " + testStationName);
            assertEquals(realStation.getTransportModes(), testStation.getTransportModes(), "mode wrong for " + testStationName);
            TestEnv.assertLatLongEquals(realStation.getLatLong(), testStation.getLatLong(), 0.001,
                    "latlong wrong for " + testStationName);
        });

    }
}
