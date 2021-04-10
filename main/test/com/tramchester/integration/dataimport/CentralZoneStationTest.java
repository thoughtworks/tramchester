package com.tramchester.integration.dataimport;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.reference.CentralZoneStation;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CentralZoneStationTest {
    private static ComponentContainer componentContainer;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        componentContainer = new ComponentsBuilder<>().create(new IntegrationTramTestConfig(), TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @Test
    void shouldHaveCorrespondanceWithLoadedStations() {
        List<CentralZoneStation> centralZoneStations = Arrays.asList(CentralZoneStation.values());

        StationRepository stationRepository = componentContainer.get(StationRepository.class);

        List<CentralZoneStation> presentInLoaded = centralZoneStations.stream().
                filter(centralStation -> stationRepository.hasStationId(centralStation.getId())).
                collect(Collectors.toList());

        assertEquals(centralZoneStations.size(), presentInLoaded.size());
    }
}
