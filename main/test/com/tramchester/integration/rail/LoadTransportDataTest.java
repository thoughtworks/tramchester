package com.tramchester.integration.rail;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.dataimport.rail.LoadRailTransportData;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.GridPosition;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.*;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class LoadTransportDataTest {
    private static ComponentContainer componentContainer;
    private TransportData loaded;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        // TODO rail config?
        IntegrationTramTestConfig configuration = new IntegrationTramTestConfig(true);
        componentContainer = new ComponentsBuilder().create(configuration, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        LoadRailTransportData loadRailTransportData = componentContainer.get(LoadRailTransportData.class);
        loaded = loadRailTransportData.getData();
    }

    @Test
    void shouldLoadStations() {
        Set<Station> allStations = loaded.getStations();
        assertFalse(allStations.isEmpty());
    }

    @Test
    void shouldGetSpecificStation() {
        Station result = loaded.getStationById(StringIdFor.createId("DRBY"));

        assertEquals("DERBY", result.getName());
        final GridPosition expectedGrid = new GridPosition(436200, 335600);
        assertEquals(expectedGrid, result.getGridPosition());

        final LatLong expectedLatLong = CoordinateTransforms.getLatLong(expectedGrid);
        assertEquals(expectedLatLong, result.getLatLong());
    }

    @Test
    void shouldGetSpecificStationWithoutPosition() {
        // A    KILLARNEY   (CIE              0KILARNYKLL   KLL00000E00000 5

        Station result = loaded.getStationById(StringIdFor.createId("KILARNY"));

        assertEquals("KILLARNEY   (CIE", result.getName());
        assertFalse(result.getGridPosition().isValid());
        assertFalse(result.getLatLong().isValid());
    }

    @Test
    void shouldFindSomething() {
        loaded.getServices();
    }
}
