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
import com.tramchester.testSupport.testTags.BusTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import static com.tramchester.domain.reference.TransportMode.*;
import static com.tramchester.integration.repository.TransportDataFromFilesTramTest.NUM_TFGM_TRAM_STATIONS;
import static com.tramchester.integration.repository.buses.TransportDataFromFilesBusTest.NUM_TFGM_BUS_STATIONS;
import static com.tramchester.integration.repository.trains.TransportDataFromFilesTrainTest.GB_RAIL_AGENCIES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@BusTest
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
public class TransportDataFromFilesAllModesTest {

    public static final int TGFM_BUS_AGENCIES = 41;
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
    void shouldHaveExpectedAgenciesNumbersForBus() {
        assertEquals(TGFM_BUS_AGENCIES+1+GB_RAIL_AGENCIES, transportData.getAgencies().size());
    }

    @Test
    void shouldHaveExpectedNumberOfTramStations() {
        assertEquals(NUM_TFGM_TRAM_STATIONS, transportData.getStationsForMode(Tram).size());
    }

    @Test
    void shouldHaveExpectedNumberOfTrainStations() {
        // number of stations within tfgm bounds
        assertEquals(266, transportData.getStationsForMode(Train).size());
    }

    @Test
    void shouldHaveExpectedNumberOfBusStations() {
        final long tfgmBuses = transportData.getStationsFromSource(DataSourceID.tfgm).filter(station -> station.serves(Bus)).count();
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
