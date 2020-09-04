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

class ValidateTestStations {

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

        testStations.forEach(testStation -> {
            Station station = TramStations.of(testStation);

            Station realStation = transportData.getStationById(station.getId());

            assertEquals(realStation.getArea(), station.getArea(),"area wrong for " +station.getName());
            assertEquals(realStation.getName(), station.getName(), "name");
            assertEquals(realStation.getTransportMode(), station.getTransportMode(), "mode wrong for " +station.getName());

            // TODO
            //assertEquals(realStation.getLatLong(), station.getLatLong());

        });

    }
}
