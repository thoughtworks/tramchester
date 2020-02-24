package com.tramchester.integration.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.tramchester.App;
import com.tramchester.domain.presentation.DTO.RouteDTO;
import com.tramchester.domain.presentation.DTO.StationDTO;
import com.tramchester.domain.presentation.ProximityGroup;
import com.tramchester.integration.IntegrationClient;
import com.tramchester.integration.IntegrationTestRun;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.integration.Stations;
import org.junit.Before;
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

    private ObjectMapper mapper = new ObjectMapper();
    private RouteDTO ashtonEcclesRoute = new RouteDTO("Ashton-under-Lyne - Manchester - Eccles",
            "shortName", new LinkedList<>(), "displayClass");

    @Before
    public void beforeEachTestRuns() {
        mapper.registerModule(new JodaModule());
    }

    @Test
    public void shouldGetAllRoutes() {
        Response result = IntegrationClient.getResponse(testRule, "routes", Optional.empty(), 200);
        List<RouteDTO> routes = result.readEntity(new GenericType<>() {});

        assertEquals(12, routes.size());

        routes.forEach(route -> assertFalse("Route no stations "+route.getRouteName(),route.getStations().isEmpty()));

        int index = routes.indexOf(ashtonEcclesRoute);
        assertTrue(index>0);

        RouteDTO ashtonRoute = routes.get(index);
        List<StationDTO> ashtonRouteStations = ashtonRoute.getStations();

        assertEquals("3", ashtonRoute.getShortName().trim());
        assertTrue(ashtonRouteStations.contains(new StationDTO(Stations.Ashton, ProximityGroup.ALL)));
        assertTrue(ashtonRouteStations.contains(new StationDTO(Stations.Eccles, ProximityGroup.ALL)));
    }
}
