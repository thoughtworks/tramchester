package com.tramchester.integration.mappers;

import com.tramchester.Dependencies;
import com.tramchester.domain.TransportMode;
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
                new LinkedList<>(), "displayClass", TransportMode.Tram);

        int index = dtos.indexOf(query);

        List<StationRefWithPosition> stations = dtos.get(index).getStations();
        StationRefWithPosition stationRefWithPosition = stations.get(0);
        Assertions.assertEquals(Stations.ManAirport.getId(), stationRefWithPosition.getId());
        Assertions.assertEquals(TestEnv.manAirportLocation, stationRefWithPosition.getLatLong());
        Assertions.assertEquals(TransportMode.Tram, stationRefWithPosition.getTransportMode());
        Assertions.assertEquals(Stations.Victoria.getId(), stations.get(stations.size()-1).getId());
    }
}
