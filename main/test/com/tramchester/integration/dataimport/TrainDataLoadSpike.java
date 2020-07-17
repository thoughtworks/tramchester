package com.tramchester.integration.dataimport;

import com.tramchester.Dependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.integration.IntegrationTrainTestConfig;
import com.tramchester.repository.TransportData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class TrainDataLoadSpike {

    private static Dependencies dependencies;
    private TransportData data;

    @BeforeAll
    static void beforeClass() throws IOException {
        TramchesterConfig testConfig = new IntegrationTrainTestConfig();

        dependencies = new Dependencies();
        dependencies.initialise(testConfig);
    }

    @BeforeEach
    void beforeEachTestRuns() {
        data = dependencies.get(TransportData.class);
    }

    @Test
    void shouldCheckStations() {
        data.getStations().forEach(station -> {
            LatLong latlong = station.getLatLong();
            assertNotEquals(0, latlong.getLat(), station.getId());
            assertNotEquals(0, latlong.getLon(), station.getId());

        });

    }
}
