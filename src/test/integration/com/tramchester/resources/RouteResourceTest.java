package com.tramchester.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.tramchester.*;
import com.tramchester.domain.presentation.DTO.RouteDTO;
import com.tramchester.domain.presentation.DTO.StationDTO;
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

    ObjectMapper mapper = new ObjectMapper();
    private RouteDTO ashtonEcclesRoute = new RouteDTO("Ashton-under-Lyne - MediaCityUK - Eccles",
            new LinkedList<>(), "displayClass");

    @Before
    public void beforeEachTestRuns() {
        mapper.registerModule(new JodaModule());
    }

    @Test
    public void shouldGetAllRoutes() {
        Response result = IntegrationClient.getResponse(testRule, String.format("routes"), Optional.empty());
        List<RouteDTO> routes = result.readEntity(new GenericType<List<RouteDTO>>(){});

        assertEquals(22, routes.size());

        routes.forEach(route -> assertFalse("Route no stations "+route.getRouteName(),route.getStations().isEmpty()));

        int index = routes.indexOf(ashtonEcclesRoute);
        assertTrue(index>0);

        RouteDTO ashtonRoute = routes.get(index);
        List<StationDTO> ashtonRouteStations = ashtonRoute.getStations();
        StationDTO firstStation = ashtonRouteStations.get(0);
        StationDTO lastStation = ashtonRouteStations.get(ashtonRouteStations.size()-1);

        assertEquals(Stations.Ashton.getId(), firstStation.getId());
        assertEquals(Stations.Eccles.getId(), lastStation.getId());
    }

}
