package com.tramchester.integration.dataimport;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.places.GroupedStations;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.repository.CompositeStationRepository;
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

import static org.junit.jupiter.api.Assertions.*;

@BusTest
class ValidateBusTestStations {

    private static ComponentContainer componentContainer;

    private StationRepository stationRepository;
    private CompositeStationRepository compositeStationRepository;

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
        compositeStationRepository = componentContainer.get(CompositeStationRepository.class);
    }

    @Test
    void shouldHaveCorrectTestBusStations() {
        List<BusStations> testStations = Arrays.asList(BusStations.values());

        testStations.forEach(enumValue -> {
            Station testStation = BusStations.of(enumValue);

            Station realStation = enumValue.from(stationRepository);

            String testStationName = testStation.getName();
            assertEquals(realStation.getName(), testStationName, "name wrong for id: " + testStation.getId());
            // area enriched/loaded from naptan data
            assertEquals(realStation.getArea(), testStation.getArea(),"area wrong for " + testStationName);
            assertEquals(realStation.getTransportModes(), testStation.getTransportModes(), "mode wrong for " + testStationName);
            TestEnv.assertLatLongEquals(realStation.getLatLong(), testStation.getLatLong(), 0.001,
                    "latlong wrong for " + testStationName);

            assertFalse(realStation.getDropoffRoutes().isEmpty(), "no drop offs " + realStation.getName());
            assertFalse(realStation.getPickupRoutes().isEmpty(), "no pick ups " + realStation.getName());
        });

    }

    @Test
    void shouldHaveCorrectTestCompositeStations() {
        List<BusStations.Composites> composites = Arrays.asList(BusStations.Composites.values());

        composites.forEach(enumValue -> {
            GroupedStations found = compositeStationRepository.findByName(enumValue.getName());
            assertNotNull(found, enumValue.getName());
            assertTrue(found.getContained().size()>1);
        });
    }
}
