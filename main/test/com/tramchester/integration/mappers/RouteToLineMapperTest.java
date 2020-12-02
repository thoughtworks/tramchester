package com.tramchester.integration.mappers;

import com.tramchester.Dependencies;
import com.tramchester.domain.liveUpdates.LineAndDirection;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.mappers.RouteToLineMapper;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TransportData;
import org.junit.jupiter.api.*;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

class RouteToLineMapperTest {
    private static Dependencies dependencies;
    private StationRepository repository;
    private RouteToLineMapper mapper;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        dependencies = new Dependencies();
        IntegrationTramTestConfig testConfig = new IntegrationTramTestConfig();
        dependencies.initialise(testConfig);
    }

    @AfterAll
    static void onceAfterAllTestsHaveRun() {
        dependencies.close();
    }

    @BeforeEach
    void beforeAnyTestsRun() {
        repository = dependencies.get(TransportData.class);
        mapper = dependencies.get(RouteToLineMapper.class);
    }

    @Disabled("WIP")
    @Test
    void shouldMapEachRouteStationToALine() {
        Set<RouteStation> routeStations = repository.getRouteStations();

        for (RouteStation routeStation : routeStations) {
            LineAndDirection lineAndDirection = mapper.map(routeStation);
            assertNotEquals(LineAndDirection.Unknown, lineAndDirection);
        }

    }

}
