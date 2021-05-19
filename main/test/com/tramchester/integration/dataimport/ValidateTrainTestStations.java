package com.tramchester.integration.dataimport;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.train.IntegrationTrainTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TrainStations;
import com.tramchester.testSupport.testTags.TrainTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TrainTest
class ValidateTrainTestStations {

    private static ComponentContainer componentContainer;

    private StationRepository stationRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        componentContainer = new ComponentsBuilder().create(new IntegrationTrainTestConfig(), TestEnv.NoopRegisterMetrics());
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
    void shouldHaveCorrectTestTrainStations() {
        List<TrainStations> testStations = Arrays.asList(TrainStations.values());

        testStations.forEach(enumValue -> {
            Station testStation = TrainStations.of(enumValue);

            Station realStation = stationRepository.getStationById(testStation.getId());

            String testStationName = testStation.getName();
            assertEquals(realStation.getName(), testStationName, "name wrong for id: " + testStation.getId());
            assertEquals(realStation.getArea(), testStation.getArea(),"area wrong for " + testStationName);
            assertEquals(realStation.getTransportModes(), testStation.getTransportModes(), "mode wrong for " + testStationName);
            assertEquals(realStation.getLatLong(), testStation.getLatLong(), "latlong wrong for " + testStationName);

        });

    }
}
