package com.tramchester.integration.resources;

import com.tramchester.App;
import com.tramchester.domain.places.ProximityGroups;
import com.tramchester.domain.presentation.DTO.RouteDTO;
import com.tramchester.domain.presentation.DTO.StationDTO;
import com.tramchester.integration.IntegrationClient;
import com.tramchester.integration.IntegrationTestRun;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.testSupport.RoutesForTesting;
import com.tramchester.testSupport.Stations;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DropwizardExtensionsSupport.class)
public class RouteResourceTest {

    public static IntegrationTestRun testRule = new IntegrationTestRun(App.class, new IntegrationTramTestConfig());

    @Test
    void shouldGetAllRoutes() {
        List<RouteDTO> routes = getRouteResponse();

        // Lockdown 14->12
        assertEquals(14, routes.size());

        routes.forEach(route -> assertFalse(route.getStations().isEmpty(), "Route no stations "+route.getRouteName()));

       RouteDTO query = new RouteDTO("Ashton-under-Lyne - Manchester - Eccles",
                "shortName", new LinkedList<>(), "displayClass");
        int index = routes.indexOf(query);
        assertTrue(index>0);

        RouteDTO ashtonRoute = routes.get(index);
        List<StationDTO> ashtonRouteStations = ashtonRoute.getStations();

        assertEquals("3", ashtonRoute.getShortName().trim());
        assertTrue(ashtonRouteStations.contains(new StationDTO(Stations.Ashton, ProximityGroups.STOPS)));
        assertTrue(ashtonRouteStations.contains(new StationDTO(Stations.Eccles, ProximityGroups.STOPS)));
    }

    @Test
    void shouldListStationsInOrder() {
        List<RouteDTO> routes = getRouteResponse();

        RouteDTO query = new RouteDTO(RoutesForTesting.AIR_TO_VIC.getName(), "shortName", new LinkedList<>(), "displayClass");
        int index = routes.indexOf(query);
        assertTrue(index>0);

        List<StationDTO> stations = routes.get(index).getStations();
        assertEquals(Stations.ManAirport.getId(), stations.get(0).getId());
        assertEquals(Stations.Victoria.getId(), stations.get(stations.size()-1).getId());
    }

    private List<RouteDTO> getRouteResponse() {
        Response result = IntegrationClient.getApiResponse(testRule, "routes", Optional.empty(), 200);
        return result.readEntity(new GenericType<>() {
        });
    }
}
