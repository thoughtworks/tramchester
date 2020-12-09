package com.tramchester.integration.dataimport;

import com.tramchester.ComponentContainer;
import com.tramchester.Dependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.integration.testSupport.IntegrationTrainTestConfig;
import com.tramchester.repository.TransportData;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.*;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class TrainDataLoadSpike {

    private static ComponentContainer componentContainer;
    private TransportData data;

    @BeforeAll
    static void beforeClass() {
        TramchesterConfig testConfig = new IntegrationTrainTestConfig();

        componentContainer = new Dependencies();
        componentContainer.initialise(testConfig);
    }

    @AfterAll
    static void afterClass() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        data = componentContainer.get(TransportData.class);
    }

    @Test
    void shouldSenseCheckData() {
        assertFalse(data.getStations().isEmpty());
        assertFalse(data.getServices().isEmpty());
        assertFalse(data.getRoutes().isEmpty());
        assertFalse(data.getAgencies().isEmpty());
        assertFalse(data.getTrips().isEmpty());

        assertEquals(0, data.getStations().stream().filter(TransportMode::isTram).count());
        assertEquals(0, data.getStations().stream().filter(TransportMode::isBus).count());

        long result = data.getStations().stream().filter(station -> station.getAgencies().size() < 2).count();
        assertNotEquals(0,result);

        // TODO some stations in train data have 0,0 positions
//        data.getStations().forEach(station -> {
//            LatLong latlong = station.getLatLong();
//            assertNotEquals(0, latlong.getLat(), station.getId());
//            assertNotEquals(0, latlong.getLon(), station.getId());
//        });

    }
}
