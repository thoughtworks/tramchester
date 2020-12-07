package com.tramchester.integration.resources;

import com.tramchester.App;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.presentation.DTO.RouteDTO;
import com.tramchester.domain.presentation.DTO.StationRefDTO;
import com.tramchester.domain.presentation.DTO.StationRefWithPosition;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.IntegrationClient;
import com.tramchester.integration.testSupport.IntegrationTramTestConfig;
import com.tramchester.testSupport.*;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.KnownRoute.AshtonunderLyneManchesterEccles;
import static com.tramchester.domain.reference.KnownRoute.ManchesterAirportVictoria;
import static com.tramchester.testSupport.RoutesForTesting.createTramRoute;
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

        RouteDTO query = new RouteDTO(createTramRoute(AshtonunderLyneManchesterEccles), new LinkedList<>());
        int index = routes.indexOf(query);
        assertTrue(index>0);

        RouteDTO ashtonRoute = routes.get(index);
        assertTrue(ashtonRoute.isTram());
        List<StationRefWithPosition> ashtonRouteStations = ashtonRoute.getStations();

        assertEquals("3", ashtonRoute.getShortName().trim());
        List<String> ids = ashtonRouteStations.stream().map(StationRefDTO::getId).collect(Collectors.toList());
        assertTrue(ids.contains(TramStations.Ashton.forDTO()));
        assertTrue(ids.contains(TramStations.Eccles.forDTO()));
    }

    @Test
    void shouldListStationsInOrder() {
        List<RouteDTO> routes = getRouteResponse();

        RouteDTO query = new RouteDTO(createTramRoute(ManchesterAirportVictoria), new LinkedList<>());
        int index = routes.indexOf(query);
        assertTrue(index>0);

        List<StationRefWithPosition> stations = routes.get(index).getStations();
        StationRefWithPosition first = stations.get(0);
        assertEquals(TramStations.ManAirport.forDTO(), first.getId());
        assertEquals(TramStations.ManAirport.getLatLong(), first.getLatLong());
        assertEquals(TransportMode.Tram, first.getTransportMode());

        assertEquals(TramStations.Victoria.forDTO(), stations.get(stations.size()-1).getId());
    }

    private List<RouteDTO> getRouteResponse() {
        Response result = IntegrationClient.getApiResponse(appExtension, "routes");
        assertEquals(200, result.getStatus());
        return result.readEntity(new GenericType<>() {
        });
    }
}
