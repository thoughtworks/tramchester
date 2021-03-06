package com.tramchester.unit.geo;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.places.Station;
import com.tramchester.geo.MarginInMeters;
import com.tramchester.geo.StationLocations;
import com.tramchester.repository.CompositeStationRepository;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramTransportDataForTestFactory;
import com.tramchester.unit.graph.calculation.SimpleCompositeGraphConfig;
import com.tramchester.unit.graph.calculation.SimpleGraphConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static com.tramchester.testSupport.TestEnv.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StationLocationsFromTestDataTest {

    private static ComponentContainer componentContainer;
    private static SimpleGraphConfig config;
    private TramTransportDataForTestFactory.TramTransportDataForTest transportData;
    private StationLocations stationLocations;
    private CompositeStationRepository compositeStationRepository;

    @BeforeAll
    static void onceBeforeAllTestRuns() throws IOException {
        config = new SimpleCompositeGraphConfig("tramroutetest.db");
        TestEnv.deleteDBIfPresent(config);

        componentContainer = new ComponentsBuilder().
                overrideProvider(TramTransportDataForTestFactory.class).
                create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
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
        compositeStationRepository = componentContainer.get(CompositeStationRepository.class);
    }

    @Test
    void shouldFindFirstStation() {
        List<Station> results = stationLocations.nearestStationsSorted(nearAltrincham, 3, MarginInMeters.of(1000));
        assertEquals(1, results.size(), results.toString());
        assertEquals(compositeStationRepository.findByName("startStation"), results.get(0));
    }

    @Test
    void shouldFindFourthStation() {
        List<Station> results = stationLocations.nearestStationsSorted(nearKnutsfordBusStation, 3, MarginInMeters.of(1000));
        assertEquals(1, results.size(), results.toString());
        assertEquals(compositeStationRepository.findByName("Station4"), results.get(0));
    }

    @Test
    void shouldFindSecondStation() {
        List<Station> results = stationLocations.nearestStationsSorted(TestEnv.nearWythenshaweHosp, 3, MarginInMeters.of(500));
        assertEquals(1, results.size(), results.toString());
        assertTrue(results.contains(transportData.getSecond()));
    }

    @Test
    void shouldFindLastStation() {
        List<Station> results = stationLocations.nearestStationsSorted(nearPiccGardens, 3, MarginInMeters.of(500));
        assertEquals(1, results.size(), results.toString());
        assertTrue(results.contains(transportData.getLast()));
    }

    @Test
    void shouldFindInterchange() {
        List<Station> results = stationLocations.nearestStationsSorted(nearShudehill, 3, MarginInMeters.of(500));
        assertEquals(1, results.size(), results.toString());
        assertTrue(results.contains(transportData.getInterchange()));
    }

    @Test
    void shouldFindNearStockport() {
        List<Station> results = stationLocations.nearestStationsSorted(nearStockportBus, 3, MarginInMeters.of(500));
        assertEquals(1, results.size(), results.toString());
        assertTrue(results.contains(transportData.getFifthStation()));
    }

}
