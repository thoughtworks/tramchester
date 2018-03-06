package com.tramchester.integration.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.tramchester.App;
import com.tramchester.domain.presentation.DTO.DepartureDTO;
import com.tramchester.integration.IntegrationClient;
import com.tramchester.integration.IntegrationTestRun;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.integration.Stations;
import org.joda.time.LocalTime;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

public class DeparturesResourceTest {

    @ClassRule
    public static IntegrationTestRun testRule = new IntegrationTestRun(App.class, new IntegrationTramTestConfig());

    ObjectMapper mapper = new ObjectMapper();

    @Before
    public void beforeEachTestRuns() {
        mapper.registerModule(new JodaModule());
    }


    // NOTE: will fail if API key not available in env var TFGMAPIKEY
    @Test
    public void shouldGetNearbyDepartures() {
        double lat = 53.4804263d;
        double lon = -2.2392436d;

        LocalTime queryTime = LocalTime.now().minusMinutes(5);

        Response response = IntegrationClient.getResponse(testRule, String.format("departures/%s/%s", lat, lon),
                Optional.empty());
        assertEquals(200,response.getStatus());
        SortedSet<DepartureDTO> departures =  response.readEntity(new GenericType<SortedSet<DepartureDTO>>(){});

        assertFalse(departures.isEmpty());
        DepartureDTO departureDTO = departures.first();
        LocalTime when = departureDTO.getWhen();
        assertTrue(when.isAfter(queryTime) );
        String nextDepart = departureDTO.getFrom();
        assertTrue(nextDepart.equals(Stations.PiccadillyGardens.getName()) || nextDepart.equals(Stations.StPetersSquare.getName()));
        assertFalse(departureDTO.getStatus().isEmpty());
        assertFalse(departureDTO.getDestination().isEmpty());

    }

}
