package com.tramchester.integration.dataimport;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.ConfigParameterResolver;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.DataUpdateTest;
import com.tramchester.testSupport.testTags.DualTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;
import java.util.List;

import static com.tramchester.testSupport.TestEnv.assertLatLongEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(ConfigParameterResolver.class)
@DualTest
@DataUpdateTest
class ValidateTramStationsTest {

    private static ComponentContainer componentContainer;

    private StationRepository transportData;

    @BeforeAll
    static void onceBeforeAnyTestsRun(TramchesterConfig tramchesterConfig) {
        componentContainer = new ComponentsBuilder().create(tramchesterConfig, TestEnv.NoopRegisterMetrics());
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
    void shouldHaveCorrectTestTramStations() {
        List<TramStations> testStations = Arrays.asList(TramStations.values());

        testStations.forEach(testStation -> {

            Station realStation = transportData.getStationById(testStation.getId());

            String testStationName = testStation.getName();

            assertEquals(realStation.getName(), testStationName, "name wrong for id: " + testStation.getId());

            assertLatLongEquals(realStation.getLatLong(), testStation.getLatLong(), 0.001, "latlong wrong for " + testStationName);

        });
    }

}
