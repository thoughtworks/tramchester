package com.tramchester.integration.dataimport;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.IntegrationBusTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.reference.BusStations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class ValidateBusTestStations {

    private static ComponentContainer componentContainer;

    private StationRepository transportData;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        componentContainer = new ComponentsBuilder().create(new IntegrationBusTestConfig());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        transportData = componentContainer.get(StationRepository.class);
    }

    @Test
    void shouldHaveCorrectTestBusStations() {
        List<BusStations> testStations = Arrays.asList(BusStations.values());

        testStations.forEach(enumValue -> {
            Station testStation = BusStations.of(enumValue);

            Station realStation = transportData.getStationById(testStation.getId());

            String testStationName = testStation.getName();
            assertEquals(realStation.getName(), testStationName, "name wrong for id: " + testStation.getId());
            assertEquals(realStation.getArea(), testStation.getArea(),"area wrong for " + testStationName);
            assertEquals(realStation.getTransportMode(), testStation.getTransportMode(), "mode wrong for " + testStationName);
            assertEquals(realStation.getLatLong(), testStation.getLatLong(), "latlong wrong for " + testStationName);

        });

    }
}
