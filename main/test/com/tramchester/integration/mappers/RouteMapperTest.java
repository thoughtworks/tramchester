package com.tramchester.integration.mappers;

import com.tramchester.Dependencies;
import com.tramchester.domain.presentation.DTO.RouteDTO;
import com.tramchester.domain.presentation.DTO.StationDTO;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.mappers.RoutesMapper;
import com.tramchester.testSupport.RoutesForTesting;
import com.tramchester.testSupport.Stations;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class RouteMapperTest {
    private static Dependencies dependencies;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws IOException {
        dependencies = new Dependencies();
        IntegrationTramTestConfig testConfig = new IntegrationTramTestConfig();
        dependencies.initialise(testConfig);
    }

    @AfterClass
    public static void onceAfterAllTestsHaveRun() {
        dependencies.close();
    }

    @Test
    public void shouldGetRouteStationsInCorrectOrder() {
        RoutesMapper mapper = dependencies.get(RoutesMapper.class);

        List<RouteDTO> dtos = mapper.getAllRoutes();
        RouteDTO query = new RouteDTO(RoutesForTesting.AIR_TO_VIC.getName(), "shortName",
                new LinkedList<>(), "displayClass");

        int index = dtos.indexOf(query);

        List<StationDTO> stations = dtos.get(index).getStations();
        assertEquals(Stations.ManAirport.getId(), stations.get(0).getId());
        assertEquals(Stations.Victoria.getId(), stations.get(stations.size()-1).getId());
    }
}
