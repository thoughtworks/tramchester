package com.tramchester.integration.dataimport;

import com.tramchester.Dependencies;
import com.tramchester.domain.reference.CentralZoneStations;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.repository.StationRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CentralZoneStationsTest {
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
        List<CentralZoneStations> centralZoneStations = Arrays.asList(CentralZoneStations.values());

        StationRepository stationRepository = dependencies.get(StationRepository.class);

        List<CentralZoneStations> presentInLoaded = centralZoneStations.stream().
                filter(centralStation -> stationRepository.hasStationId(centralStation.getId())).
                collect(Collectors.toList());

        assertEquals(centralZoneStations.size(), presentInLoaded.size());
    }
}
