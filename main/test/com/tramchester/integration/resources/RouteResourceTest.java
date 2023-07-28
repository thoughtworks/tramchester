package com.tramchester.integration.resources;

import com.tramchester.App;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.presentation.DTO.LocationRefDTO;
import com.tramchester.domain.presentation.DTO.LocationRefWithPosition;
import com.tramchester.domain.presentation.DTO.RouteDTO;
import com.tramchester.domain.presentation.DTO.RouteRefDTO;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.integration.testSupport.APIClient;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.tram.ResourceTramTestConfig;
import com.tramchester.repository.RouteRepository;
import com.tramchester.resources.RouteResource;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.reference.KnownTramRoute;
import com.tramchester.testSupport.reference.TramStations;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.apache.commons.collections4.SetUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.TestEnv.dateFormatDashes;
import static com.tramchester.testSupport.reference.KnownTramRoute.ManchesterAirportWythenshaweVictoria;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DropwizardExtensionsSupport.class)
class RouteResourceTest {

    private static final IntegrationAppExtension appExtension = new IntegrationAppExtension(App.class,
            new ResourceTramTestConfig<>(RouteResource.class));
    private RouteRepository routeRepository;
    private TramRouteHelper tramRouteHelper;

    @BeforeEach
    void onceBeforeEachTestRuns() {
        App app =  appExtension.getApplication();
        routeRepository = app.getDependencies().get(RouteRepository.class);
        tramRouteHelper = new TramRouteHelper(routeRepository);
    }

    @Test
    void shouldGetAllRoutes() {

        TramDate today = TramDate.from(TestEnv.LocalNow());

        Route expectedAirportRoute = tramRouteHelper.getOneRoute(ManchesterAirportWythenshaweVictoria, today);

        List<RouteDTO> routes = getRouteResponse(); // uses current date server side

        Set<String> fromRepos = routes.stream().map(RouteRefDTO::getRouteName).collect(Collectors.toSet());

        Set<String> onDate = KnownTramRoute.getFor(today).stream().map(KnownTramRoute::longName).collect(Collectors.toSet());

        Set<String> mismatch = SetUtils.disjunction(fromRepos, onDate);

        assertTrue(mismatch.isEmpty());

        routes.forEach(route -> assertFalse(route.getStations().isEmpty(), "Route no stations "+route.getRouteName()));

        RouteDTO airportRoute = routes.stream().
                filter(routeDTO -> routeDTO.getRouteID().equals(IdForDTO.createFor(expectedAirportRoute))).
                findFirst().orElseThrow();

        assertTrue(airportRoute.isTram());
        assertEquals("Navy Line", airportRoute.getShortName().trim());

        List<LocationRefWithPosition> airportRouteStations = airportRoute.getStations();

        List<IdForDTO> ids = airportRouteStations.stream().
                map(LocationRefDTO::getId).
                collect(Collectors.toList());

        assertTrue(ids.contains(TramStations.ManAirport.getIdForDTO()));

    }

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
        assertEquals(TramStations.ManAirport.getRawId(), first.getId().getActualId());
        TestEnv.assertLatLongEquals(TramStations.ManAirport.getLatLong(), first.getLatLong(), 0.00001, "lat long");
        assertTrue(first.getTransportModes().contains(TransportMode.Tram));

        assertEquals(TramStations.Victoria.getRawId(), stations.get(stations.size()-1).getId().getActualId());
    }

    @Test
    void shouldGetRoutesFilteredByDate() {
        TramDate date = TestEnv.testDay();

        Set<Route> expected = routeRepository.getRoutesRunningOn(date);

        String queryString = String.format("routes/filtered?date=%s", date.format(dateFormatDashes));

        Response result = APIClient.getApiResponse(appExtension, queryString);
        assertEquals(200, result.getStatus());

        List<RouteDTO> results = result.readEntity(new GenericType<>() {});

        assertFalse(results.isEmpty());

        assertEquals(expected.size(), results.size());


    }

    private List<RouteDTO> getRouteResponse() {
        Response result = APIClient.getApiResponse(appExtension, "routes");
        assertEquals(200, result.getStatus());
        return result.readEntity(new GenericType<>() {
        });
    }
}
