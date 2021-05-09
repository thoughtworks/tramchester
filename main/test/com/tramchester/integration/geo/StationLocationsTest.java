package com.tramchester.integration.geo;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.places.Station;
import com.tramchester.geo.BoundingBox;
import com.tramchester.geo.StationLocations;
import com.tramchester.integration.testSupport.tram.TramWithPostcodesEnabled;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class StationLocationsTest {

    private static ComponentContainer componentContainer;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        TramWithPostcodesEnabled config = new TramWithPostcodesEnabled();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @Test
    void shouldHaveAllStationsContainedWithBounds() {
        StationRepository stationRepository = componentContainer.get(StationRepository.class);
        StationLocations locations = componentContainer.get(StationLocations.class);

        Set<Station> allStations = stationRepository.getStations();
        BoundingBox stationBounds = locations.getBounds();

        allStations.forEach(station -> assertTrue(stationBounds.contained(station.getGridPosition()),
                station.getId().toString()));
    }

}
