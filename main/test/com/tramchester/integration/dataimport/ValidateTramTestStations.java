package com.tramchester.integration.dataimport;

import com.tramchester.Dependencies;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TramStations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ValidateTramTestStations {

    private static Dependencies dependencies;

    private StationRepository transportData;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        dependencies = new Dependencies();
        dependencies.initialise(new IntegrationTramTestConfig());
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        transportData = dependencies.get(StationRepository.class);
    }

    @Test
    void shouldHaveCorrectTestTramStations() {
        List<TramStations> testStations = Arrays.asList(TramStations.values());

        testStations.forEach(enumValue -> {
            Station testStation = TramStations.of(enumValue);

            Station realStation = transportData.getStationById(testStation.getId());

            String testStationName = testStation.getName();
            assertEquals(realStation.getName(), testStationName, "name wrong for id: " + testStation.getId());
            assertEquals(realStation.getArea(), testStation.getArea(), "area wrong for " + testStationName);
            assertEquals(realStation.getTransportMode(), testStation.getTransportMode(), "mode wrong for " + testStationName);
            assertEquals(realStation.getLatLong(), testStation.getLatLong(), "latlong wrong for " + testStationName);

        });

    }
}
