package com.tramchester.unit.geo;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.MarginInMeters;
import com.tramchester.geo.StationLocations;
import com.tramchester.repository.StationGroupsRepository;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramTransportDataForTestFactory;
import com.tramchester.unit.graph.calculation.SimpleCompositeGraphConfig;
import com.tramchester.unit.graph.calculation.SimpleGraphConfig;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static com.tramchester.testSupport.reference.KnownLocations.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StationLocationsFromTestDataTest {

    private static ComponentContainer componentContainer;
    private static SimpleGraphConfig config;
    private static Set<TransportMode> modes;
    private TramTransportDataForTestFactory.TramTransportDataForTest transportData;
    private StationLocations stationLocations;
    private StationGroupsRepository compositeStationRepository;

    @BeforeAll
    static void onceBeforeAllTestRuns() throws IOException {
        config = new SimpleCompositeGraphConfig("tramroutetest.db");
        TestEnv.deleteDBIfPresent(config);

        componentContainer = new ComponentsBuilder().
                overrideProvider(TramTransportDataForTestFactory.class).
                create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        modes = config.getTransportModes();
    }

    @AfterAll
    static void onceAfterAllTestsRun() throws IOException {
        TestEnv.clearDataCache(componentContainer);
        componentContainer.close();
        TestEnv.deleteDBIfPresent(config);
    }

    @BeforeEach
    void beforeEachTestRuns() {
        transportData = (TramTransportDataForTestFactory.TramTransportDataForTest) componentContainer.get(TransportData.class);
        stationLocations = componentContainer.get(StationLocations.class);
        compositeStationRepository = componentContainer.get(StationGroupsRepository.class);
    }

    @Disabled("Need way to inject naptan test data here")
    @Test
    void shouldFindFirstStation() {
        List<Station> results = stationLocations.nearestStationsSorted(nearAltrincham.getGridPosition(), 3,
                MarginInMeters.of(1000), modes);
        assertEquals(1, results.size(), results.toString());

        // fallback name, no naptan area data loaded
        assertEquals(compositeStationRepository.findByName("Id{'area1'}"), results.get(0));
    }

    @Disabled("Need way to inject naptan test data here")
    @Test
    void shouldFindFourthStation() {
        List<Station> results = stationLocations.nearestStationsSorted(nearKnutsfordBusStation.location(), 3,
                MarginInMeters.of(1000), modes);
        assertEquals(1, results.size(), results.toString());

        // fallback name, no naptan area data loaded
        assertEquals(compositeStationRepository.findByName("Id{'area4'}"), results.get(0));
    }

    @Test
    void shouldFindSecondStation() {
        List<Station> results = stationLocations.nearestStationsSorted(nearWythenshaweHosp.location(), 3,
                MarginInMeters.of(500), modes);
        assertEquals(1, results.size(), results.toString());
        assertTrue(results.contains(transportData.getSecond()));
    }

    @Test
    void shouldFindLastStation() {
        List<Station> results = stationLocations.nearestStationsSorted(nearPiccGardens.location(), 3,
                MarginInMeters.of(500), modes);
        assertEquals(1, results.size(), results.toString());
        assertTrue(results.contains(transportData.getLast()));
    }

    @Test
    void shouldFindInterchange() {
        List<Station> results = stationLocations.nearestStationsSorted(nearShudehill.location(), 3,
                MarginInMeters.of(500), modes);
        assertEquals(1, results.size(), results.toString());
        assertTrue(results.contains(transportData.getInterchange()));
    }

    @Test
    void shouldFindNearStockport() {
        List<Station> results = stationLocations.nearestStationsSorted(nearStockportBus.location(), 3,
                MarginInMeters.of(500), modes);
        assertEquals(1, results.size(), results.toString());
        assertTrue(results.contains(transportData.getFifthStation()));
    }

}
