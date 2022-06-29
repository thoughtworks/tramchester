package com.tramchester.integration.repository.allModes;


import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.AllModesTestConfig;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.BusStations;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import static com.tramchester.domain.reference.TransportMode.*;
import static com.tramchester.integration.repository.TransportDataFromFilesTramTest.NUM_TFGM_TRAM_STATIONS;
import static com.tramchester.integration.repository.buses.TransportDataFromFilesBusTest.NUM_TFGM_BUS_STATIONS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled("All modes is WIP currently")
public class TransportDataFromFilesAllModesTest {

    private static ComponentContainer componentContainer;

    private TransportData transportData;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        TramchesterConfig config = new AllModesTestConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        transportData = componentContainer.get(TransportData.class);
    }

    @Test
    void shouldHaveExpectedNumberOfTramStations() {
        assertEquals(NUM_TFGM_TRAM_STATIONS, transportData.getStationsServing(Tram).size());
    }

    @Test
    void shouldHaveExpectedNumberOfTrainStations() {
        // number of stations within tfgm bounds
        assertEquals(266, transportData.getStationsServing(Train).size());
    }

    @Test
    void shouldHaveExpectedNumberOfBusStations() {
        final long tfgmBuses = transportData.getStationsFromSource(DataSourceID.tfgm).filter(station -> station.servesMode(Bus)).count();
        assertEquals(NUM_TFGM_BUS_STATIONS, tfgmBuses);
    }

    @Test
    void shouldHaveKnownBusStations() {

        for(BusStations testStation : BusStations.values()) {
            assertTrue(transportData.hasStationId(testStation.getId()), testStation.name());
            Station found = transportData.getStationById(testStation.getId());
            assertEquals(testStation.getName(), found.getName());
        }
    }

    @Test
    void shouldHaveKnownTramStations() {
        for(TramStations testStation : TramStations.values()) {
            assertTrue(transportData.hasStationId(testStation.getId()), testStation.name());
            Station found = transportData.getStationById(testStation.getId());
            assertEquals(testStation.getName(), found.getName());
        }
    }



}
