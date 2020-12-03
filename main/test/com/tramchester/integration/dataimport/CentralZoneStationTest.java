package com.tramchester.integration.dataimport;

import com.tramchester.Dependencies;
import com.tramchester.domain.reference.CentralZoneStation;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.repository.StationRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CentralZoneStationTest {
    private static Dependencies dependencies;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        dependencies = new Dependencies();
        dependencies.initialise(new IntegrationTramTestConfig());
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Test
    void shouldHaveCorrespondanceWithLoadedStations() {
        List<CentralZoneStation> centralZoneStations = Arrays.asList(CentralZoneStation.values());

        StationRepository stationRepository = dependencies.get(StationRepository.class);

        List<CentralZoneStation> presentInLoaded = centralZoneStations.stream().
                filter(centralStation -> stationRepository.hasStationId(centralStation.getId())).
                collect(Collectors.toList());

        assertEquals(centralZoneStations.size(), presentInLoaded.size());
    }
}
