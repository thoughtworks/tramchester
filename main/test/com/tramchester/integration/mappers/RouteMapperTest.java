package com.tramchester.integration.mappers;

import com.tramchester.Dependencies;
import com.tramchester.domain.presentation.DTO.RouteDTO;
import com.tramchester.domain.presentation.DTO.StationDTO;
import com.tramchester.domain.presentation.DTO.StationRefWithPosition;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.mappers.RoutesMapper;
import com.tramchester.testSupport.RoutesForTesting;
import com.tramchester.testSupport.Stations;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

class RouteMapperTest {
    private static Dependencies dependencies;

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws IOException {
        dependencies = new Dependencies();
        IntegrationTramTestConfig testConfig = new IntegrationTramTestConfig();
        dependencies.initialise(testConfig);
    }

    @AfterAll
    static void onceAfterAllTestsHaveRun() {
        dependencies.close();
    }

    @Test
    void shouldGetRouteStationsInCorrectOrder() {
        RoutesMapper mapper = dependencies.get(RoutesMapper.class);

        List<RouteDTO> dtos = mapper.getAllRoutes();
        RouteDTO query = new RouteDTO(RoutesForTesting.AIR_TO_VIC.getName(), "shortName",
                new LinkedList<>(), "displayClass", true);

        int index = dtos.indexOf(query);

        List<StationRefWithPosition> stations = dtos.get(index).getStations();
        Assertions.assertEquals(Stations.ManAirport.getId(), stations.get(0).getId());
        Assertions.assertEquals(TestEnv.manAirportLocation, stations.get(0).getLatLong());
        Assertions.assertTrue(stations.get(0).isTram());
        Assertions.assertEquals(Stations.Victoria.getId(), stations.get(stations.size()-1).getId());
    }
}
