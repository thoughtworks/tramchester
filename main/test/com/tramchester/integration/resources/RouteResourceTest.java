package com.tramchester.integration.resources;

import com.tramchester.App;
import com.tramchester.domain.presentation.DTO.RouteDTO;
import com.tramchester.domain.presentation.DTO.LocationRefDTO;
import com.tramchester.domain.presentation.DTO.LocationRefWithPosition;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.integration.testSupport.APIClient;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.tram.ResourceTramTestConfig;
import com.tramchester.resources.RouteResource;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.KnownTramRoute;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.Summer2022;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.reference.KnownTramRoute.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DropwizardExtensionsSupport.class)
class RouteResourceTest {

    private static final IntegrationAppExtension appExtension = new IntegrationAppExtension(App.class,
            new ResourceTramTestConfig<>(RouteResource.class));

    @Summer2022
    @Test
    void shouldGetAllRoutes() {
        List<RouteDTO> routes = getRouteResponse();

        // +4 more replacement routes
        assertEquals(16 +4, routes.size(), "Wrong size");

        routes.forEach(route -> assertFalse(route.getStations().isEmpty(), "Route no stations "+route.getRouteName()));

        RouteDTO ashtonRoute = routes.stream().
                filter(routeDTO -> routeDTO.getShortName().equals(AshtonUnderLyneManchesterEccles.shortName())).
                findFirst().orElseThrow();

        assertTrue(ashtonRoute.isTram());
        List<LocationRefWithPosition> ashtonRouteStations = ashtonRoute.getStations();

        assertEquals("Blue Line", ashtonRoute.getShortName().trim());
        List<String> ids = ashtonRouteStations.stream().map(LocationRefDTO::getId).collect(Collectors.toList());
        assertTrue(ids.contains(TramStations.Ashton.getRawId()));

        // Summer 2022 - eccles is on the replacement bus route
        //assertTrue(ids.contains(TramStations.Eccles.getRawId()));
    }

    @Summer2022
    @Test
    void shouldListStationsInOrder() {
        List<RouteDTO> routes = getRouteResponse();

        // TODO could be a mistake in the data, but the station order is flipped Summer2020, was Airport->Victoria route
        List<RouteDTO> airRoutes = routes.stream().
                filter(routeDTO -> routeDTO.getRouteName().equals(ManchesterAirportWythenshaweVictoria.longName())).
                collect(Collectors.toList());

        assertEquals(1, airRoutes.size());

        RouteDTO airRoute = airRoutes.get(0);
        List<LocationRefWithPosition> stations = airRoute.getStations();

        LocationRefWithPosition first = stations.get(0);
        assertEquals(TramStations.ManAirport.getRawId(), first.getId());
        TestEnv.assertLatLongEquals(TramStations.ManAirport.getLatLong(), first.getLatLong(), 0.00001, "lat long");
        assertTrue(first.getTransportModes().contains(TransportMode.Tram));

        assertEquals(TramStations.Victoria.getRawId(), stations.get(stations.size()-1).getId());
    }

    private List<RouteDTO> getRouteResponse() {
        Response result = APIClient.getApiResponse(appExtension, "routes");
        assertEquals(200, result.getStatus());
        return result.readEntity(new GenericType<>() {
        });
    }
}
