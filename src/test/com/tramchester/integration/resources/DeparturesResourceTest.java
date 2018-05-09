package com.tramchester.integration.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.tramchester.App;
import com.tramchester.domain.Station;
import com.tramchester.domain.presentation.DTO.DepartureDTO;
import com.tramchester.domain.presentation.DTO.DepartureListDTO;
import com.tramchester.integration.IntegrationClient;
import com.tramchester.integration.IntegrationTestRun;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.integration.Stations;
import org.joda.time.LocalTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
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

    private List<String> nearby = Arrays.asList(new String[]{
            Stations.PiccadillyGardens.getName(),
            Stations.StPetersSquare.getName(),
            Stations.Piccadilly.getName(),
            Stations.MarketStreet.getName(),
            Stations.ExchangeSquare.getName(),
            "Shudehill"
        });

    @Before
    public void beforeEachTestRuns() {
        mapper.registerModule(new JodaModule());
    }


    // NOTE: will fail if API key not available in env var TFGMAPIKEY
    @Test
    public void shouldGetNearbyDeparturesWithNotes() {
        double lat = 53.4804263d;
        double lon = -2.2392436d;

        LocalTime queryTime = LocalTime.now().minusMinutes(5);

        Response response = IntegrationClient.getResponse(testRule, String.format("departures/%s/%s", lat, lon),
                Optional.empty());
        assertEquals(200,response.getStatus());

        DepartureListDTO departureList = response.readEntity(DepartureListDTO.class);
        //SortedSet<DepartureDTO> departures =  response.readEntity(new GenericType<SortedSet<DepartureDTO>>(){});

        SortedSet<DepartureDTO> departures = departureList.getDepartures();
        assertFalse(departures.isEmpty());
        DepartureDTO departureDTO = departures.first();
        LocalTime when = departureDTO.getWhen();
        assertTrue(when.isAfter(queryTime) );
        String nextDepart = departureDTO.getFrom();
        assertTrue(nextDepart,nearby.contains(nextDepart));
        assertFalse(departureDTO.getStatus().isEmpty());
        assertFalse(departureDTO.getDestination().isEmpty());

        List<String> notes = departureList.getNotes();
        Assert.assertFalse(notes.isEmpty());
        // ignore closure message which is always present, also if today is weekend exclude that
        int ignore = 1;
        DayOfWeek dayOfWeek = LocalDate.now().getDayOfWeek();
        if (dayOfWeek.equals(DayOfWeek.SATURDAY) || dayOfWeek.equals(DayOfWeek.SUNDAY)) {
            ignore++;
        }
        Assert.assertTrue((notes.size())-ignore>0);

    }

}
