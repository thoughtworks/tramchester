package com.tramchester.integration.geo;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
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

import static com.tramchester.testSupport.reference.KnownLocations.*;
import static org.junit.jupiter.api.Assertions.*;

public class StationLocationsTest {

    private static ComponentContainer componentContainer;
    private static TramchesterConfig config;
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

        assertTrue(fullBoundsOfAllTramStations.contained(nearShudehill.grid()));
        assertTrue(fullBoundsOfAllTramStations.contained(nearPiccGardens.grid()));

        assertTrue(fullBoundsOfAllTramStations.within(margin, nearAltrincham.grid()));

        assertFalse(fullBoundsOfAllTramStations.contained(nearGreenwichLondon.grid()));
    }

    @Test
    void shouldHaveExpectedStationLocations() {
        MarginInMeters margin = MarginInMeters.of(config.getNearestStopForWalkingRangeKM());

        assertTrue(locations.withinRangeOfStation(nearShudehill.grid(), margin));
        assertTrue(locations.withinRangeOfStation(nearPiccGardens.grid(), margin));

        assertTrue(locations.withinRangeOfStation(nearAltrincham.grid(), margin));

        assertFalse(locations.withinRangeOfStation(nearGreenwichLondon.grid(), margin));
        assertFalse(locations.withinRangeOfStation(CoordinateTransforms.getGridPosition(TestEnv.nearStockportBus), margin));
    }

    @Test
    void shouldGetBoundsForTrams() {
        BoundingBox box = locations.getBounds();

        assertEquals(376982, box.getMinEastings());
        assertEquals(385428, box.getMinNorthings());
        assertEquals(394163, box.getMaxEasting());
        assertEquals(413433, box.getMaxNorthings());

    }

}
