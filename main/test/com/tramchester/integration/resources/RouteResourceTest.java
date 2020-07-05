package com.tramchester.integration.resources;

import com.tramchester.App;
import com.tramchester.domain.places.ProximityGroups;
import com.tramchester.domain.presentation.DTO.RouteDTO;
import com.tramchester.domain.presentation.DTO.StationDTO;
import com.tramchester.domain.presentation.DTO.StationRefDTO;
import com.tramchester.domain.presentation.DTO.StationRefWithPosition;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.integration.IntegrationAppExtension;
import com.tramchester.integration.IntegrationClient;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.testSupport.RoutesForTesting;
import com.tramchester.testSupport.Stations;
import com.tramchester.testSupport.TestEnv;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DropwizardExtensionsSupport.class)
class RouteResourceTest {

    private static final IntegrationAppExtension appExtension = new IntegrationAppExtension(App.class, new IntegrationTramTestConfig());

    @Test
    void shouldGetAllRoutes() {
        List<RouteDTO> routes = getRouteResponse();

        // todo Lockdown 14->12
        assertEquals(12, routes.size());

        routes.forEach(route -> assertFalse(route.getStations().isEmpty(), "Route no stations "+route.getRouteName()));

        RouteDTO query = new RouteDTO("Ashton-under-Lyne - Manchester - Eccles",
                "shortName", new LinkedList<>(), "displayClass");
        int index = routes.indexOf(query);
        assertTrue(index>0);

        RouteDTO ashtonRoute = routes.get(index);
        List<StationRefWithPosition> ashtonRouteStations = ashtonRoute.getStations();

        assertEquals("3", ashtonRoute.getShortName().trim());
        List<String> ids = ashtonRouteStations.stream().map(StationRefDTO::getId).collect(Collectors.toList());
        assertTrue(ids.contains(Stations.Ashton.getId()));
        assertTrue(ids.contains(Stations.Eccles.getId()));
    }

    @Test
    void shouldListStationsInOrder() {
        List<RouteDTO> routes = getRouteResponse();

        RouteDTO query = new RouteDTO(RoutesForTesting.AIR_TO_VIC.getName(), "shortName", new LinkedList<>(), "displayClass");
        int index = routes.indexOf(query);
        assertTrue(index>0);

        List<StationRefWithPosition> stations = routes.get(index).getStations();
        StationRefWithPosition first = stations.get(0);
        assertEquals(Stations.ManAirport.getId(), first.getId());
        assertEquals(TestEnv.manAirportLocation, first.getLatLong());
        assertTrue(first.isTram());

        assertEquals(Stations.Victoria.getId(), stations.get(stations.size()-1).getId());
    }

    private List<RouteDTO> getRouteResponse() {
        Response result = IntegrationClient.getApiResponse(appExtension, "routes");
        assertEquals(200, result.getStatus());
        return result.readEntity(new GenericType<>() {
        });
    }
}
