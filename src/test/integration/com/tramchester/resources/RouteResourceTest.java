package com.tramchester.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.tramchester.App;
import com.tramchester.IntegrationClient;
import com.tramchester.IntegrationTestRun;
import com.tramchester.IntegrationTramTestConfig;
import com.tramchester.domain.presentation.DTO.RouteDTO;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RouteResourceTest {

    @ClassRule
    public static IntegrationTestRun testRule = new IntegrationTestRun(App.class, new IntegrationTramTestConfig());

    ObjectMapper mapper = new ObjectMapper();

    @Before
    public void beforeEachTestRuns() {
        mapper.registerModule(new JodaModule());
    }

    @Test
    public void shouldGetAllRoutes() {
        Response result = IntegrationClient.getResponse(testRule, String.format("routes"), Optional.empty());
        List<RouteDTO> routes = result.readEntity(new GenericType<List<RouteDTO>>(){});

        assertEquals(22, routes.size());

        routes.forEach(route -> assertFalse(route.getStations().isEmpty()));
    }

}
