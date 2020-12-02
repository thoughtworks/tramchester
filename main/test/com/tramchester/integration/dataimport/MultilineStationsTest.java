package com.tramchester.integration.dataimport;

import com.tramchester.Dependencies;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.MultilineStations;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.repository.StationRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultilineStationsTest {
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
        List<MultilineStations> multilineStations = Arrays.asList(MultilineStations.values());

        StationRepository routeRepository = dependencies.get(StationRepository.class);
        Set<Station> allStations = routeRepository.getStations();
        Set<Station> multiRoute = allStations.stream().filter(station -> station.getRoutes().size() > 2).collect(Collectors.toSet());

        Set<String> multilineStationIds = multilineStations.stream().map(MultilineStations::getId).collect(Collectors.toSet());
        Set<Station> missing = multiRoute.stream().filter(station -> !multilineStationIds.contains(station.getId().forDTO())).collect(Collectors.toSet());
        assertTrue(missing.isEmpty(), missing.toString());

        Set<String> multiRouteIds = multiRoute.stream().map(station -> station.getId().forDTO()).collect(Collectors.toSet());
        Set<String> extra = multilineStationIds.stream().filter(item -> !multiRouteIds.contains(item)).collect(Collectors.toSet());
        assertTrue(extra.isEmpty(), extra.toString());

        assertEquals(multilineStations.size(), multiRoute.size());

    }
}
