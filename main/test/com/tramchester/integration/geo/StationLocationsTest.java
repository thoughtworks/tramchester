package com.tramchester.integration.geo;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.places.Station;
import com.tramchester.geo.BoundingBox;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.MarginInMeters;
import com.tramchester.geo.StationLocations;
import com.tramchester.integration.testSupport.tram.TramWithPostcodesEnabled;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StationLocationsTest {

    private static ComponentContainer componentContainer;
    private static TramWithPostcodesEnabled config;
    private StationLocations locations;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        config = new TramWithPostcodesEnabled();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        locations = componentContainer.get(StationLocations.class);
    }

    @Test
    void shouldHaveAllStationsContainedWithBounds() {
        StationRepository stationRepository = componentContainer.get(StationRepository.class);

        Set<Station> allStations = stationRepository.getStations();
        BoundingBox stationBounds = locations.getBounds();

        allStations.forEach(station -> assertTrue(stationBounds.contained(station.getGridPosition()),
                station.getId().toString()));
    }

    @Test
    void shouldHaveLocationsInBounds() {
        MarginInMeters margin = MarginInMeters.of(config.getNearestStopForWalkingRangeKM());
        final BoundingBox fullBoundsOfAllTramStations = locations.getBounds();

        assertTrue(fullBoundsOfAllTramStations.contained(TestEnv.nearShudehillGrid));
        assertTrue(fullBoundsOfAllTramStations.contained(TestEnv.nearPiccGardensGrid));

        assertTrue(fullBoundsOfAllTramStations.within(margin, TestEnv.nearAltrinchamGrid));

        assertFalse(fullBoundsOfAllTramStations.contained(TestEnv.nearGreenwichGrid));
    }

    @Test
    void shouldHaveExpectedStationLocations() {
        assertTrue(locations.withinWalkingDistance(TestEnv.nearShudehillGrid));
        assertTrue(locations.withinWalkingDistance(TestEnv.nearPiccGardensGrid));

        assertTrue(locations.withinWalkingDistance(TestEnv.nearAltrinchamGrid));

        assertFalse(locations.withinWalkingDistance(TestEnv.nearGreenwichGrid));
        assertFalse(locations.withinWalkingDistance(CoordinateTransforms.getGridPosition(TestEnv.nearStockportBus)));
    }

}
