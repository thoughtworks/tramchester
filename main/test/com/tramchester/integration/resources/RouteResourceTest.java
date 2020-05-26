package com.tramchester.integration.resources;

import com.tramchester.App;
import com.tramchester.domain.Route;
import com.tramchester.domain.presentation.DTO.RouteDTO;
import com.tramchester.domain.presentation.DTO.StationDTO;
import com.tramchester.domain.presentation.ProximityGroup;
import com.tramchester.integration.IntegrationClient;
import com.tramchester.integration.IntegrationTestRun;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.testSupport.RoutesForTesting;
import com.tramchester.testSupport.Stations;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

public class RouteResourceTest {

    @ClassRule
    public static IntegrationTestRun testRule = new IntegrationTestRun(App.class, new IntegrationTramTestConfig());

    @Test
    public void shouldGetAllRoutes() {
        List<RouteDTO> routes = getRouteResponse();

        // TODO Lockdown 14->12
        assertEquals(12, routes.size());

        routes.forEach(route -> assertFalse("Route no stations "+route.getRouteName(),route.getStations().isEmpty()));

       RouteDTO query = new RouteDTO("Ashton-under-Lyne - Manchester - Eccles",
                "shortName", new LinkedList<>(), "displayClass");
        int index = routes.indexOf(query);
        assertTrue(index>0);

        RouteDTO ashtonRoute = routes.get(index);
        List<StationDTO> ashtonRouteStations = ashtonRoute.getStations();

        assertEquals("3", ashtonRoute.getShortName().trim());
        assertTrue(ashtonRouteStations.contains(new StationDTO(Stations.Ashton, ProximityGroup.ALL)));
        assertTrue(ashtonRouteStations.contains(new StationDTO(Stations.Eccles, ProximityGroup.ALL)));
    }

    @Test
    public void shouldListStationsInOrder() {
        List<RouteDTO> routes = getRouteResponse();

        RouteDTO query = new RouteDTO(RoutesForTesting.AIR_TO_VIC.getName(), "shortName", new LinkedList<>(), "displayClass");
        int index = routes.indexOf(query);
        assertTrue(index>0);

        List<StationDTO> stations = routes.get(index).getStations();
        assertEquals(Stations.ManAirport.getId(), stations.get(0).getId());
        assertEquals(Stations.Victoria.getId(), stations.get(stations.size()-1).getId());
    }

    private List<RouteDTO> getRouteResponse() {
        Response result = IntegrationClient.getResponse(testRule, "routes", Optional.empty(), 200);
        return result.readEntity(new GenericType<>() {
        });
    }
}
