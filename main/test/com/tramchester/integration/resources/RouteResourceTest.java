package com.tramchester.integration.resources;

import com.tramchester.App;
import com.tramchester.domain.presentation.DTO.RouteDTO;
import com.tramchester.domain.presentation.DTO.StationRefDTO;
import com.tramchester.domain.presentation.DTO.StationRefWithPosition;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.APIClient;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.reference.KnownTramRoute.AshtonUnderLyneManchesterEccles;
import static com.tramchester.testSupport.reference.KnownTramRoute.ManchesterAirportWythenshaweVictoria;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DropwizardExtensionsSupport.class)
class RouteResourceTest {

    private static final IntegrationAppExtension appExtension = new IntegrationAppExtension(App.class, new IntegrationTramTestConfig());

    @Test
    void shouldGetAllRoutes() {
        List<RouteDTO> routes = getRouteResponse();

        assertEquals(14, routes.size(), "Wrong size");

        routes.forEach(route -> assertFalse(route.getStations().isEmpty(), "Route no stations "+route.getRouteName()));

        RouteDTO ashtonRoute = routes.stream().
                filter(routeDTO -> routeDTO.getShortName().equals(AshtonUnderLyneManchesterEccles.shortName())).
                findFirst().orElseThrow();

        assertTrue(ashtonRoute.isTram());
        List<StationRefWithPosition> ashtonRouteStations = ashtonRoute.getStations();

        assertEquals("3BLU", ashtonRoute.getShortName().trim());
        List<String> ids = ashtonRouteStations.stream().map(StationRefDTO::getId).collect(Collectors.toList());
        assertTrue(ids.contains(TramStations.Ashton.forDTO()));
        assertTrue(ids.contains(TramStations.Eccles.forDTO()));
    }

    @Test
    void shouldListStationsInOrder() {
        List<RouteDTO> routes = getRouteResponse();

        List<RouteDTO> airRoutes = routes.stream().
                filter(routeDTO -> routeDTO.getRouteName().equals(ManchesterAirportWythenshaweVictoria.longName())).
                collect(Collectors.toList());

        assertEquals(1, airRoutes.size());

        RouteDTO airRoute = airRoutes.get(0);
        List<StationRefWithPosition> stations = airRoute.getStations();
        StationRefWithPosition first = stations.get(0);
        assertEquals(TramStations.ManAirport.forDTO(), first.getId());
        TestEnv.assertLatLongEquals(TramStations.ManAirport.getLatLong(), first.getLatLong(), 0.00001, "lat long");
        assertTrue(first.getTransportModes().contains(TransportMode.Tram));

        assertEquals(TramStations.Victoria.forDTO(), stations.get(stations.size()-1).getId());
    }

    private List<RouteDTO> getRouteResponse() {
        Response result = APIClient.getApiResponse(appExtension, "routes");
        assertEquals(200, result.getStatus());
        return result.readEntity(new GenericType<>() {
        });
    }
}
