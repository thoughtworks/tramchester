package com.tramchester.integration.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.tramchester.App;
import com.tramchester.LiveDataTestCategory;
import com.tramchester.domain.TramTime;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.presentation.DTO.DepartureDTO;
import com.tramchester.domain.presentation.DTO.DepartureListDTO;
import com.tramchester.healthchecks.LiveDataHealthCheck;
import com.tramchester.integration.IntegrationClient;
import com.tramchester.integration.IntegrationTestRun;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.integration.Stations;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import javax.ws.rs.core.Response;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;

import static junit.framework.TestCase.*;

public class DeparturesResourceTest {

    @ClassRule
    public static IntegrationTestRun testRule = new IntegrationTestRun(App.class, new IntegrationTramTestConfig());

    ObjectMapper mapper = new ObjectMapper();

    private List<String> nearby = Arrays.asList(Stations.PiccadillyGardens.getName(),
            Stations.StPetersSquare.getName(),
            Stations.Piccadilly.getName(),
            Stations.MarketStreet.getName(),
            Stations.ExchangeSquare.getName(),
            "Shudehill");

    @Before
    public void beforeEachTestRuns() {
        mapper.registerModule(new JodaModule());
    }


    @Test
    @Category(LiveDataTestCategory.class)
    public void shouldGetNearbyDeparturesWithNotes() throws TramchesterException {
        double lat = 53.4804263d;
        double lon = -2.2392436d;

        TramTime queryTime = TramTime.now().minusMinutes(5);

        Response response = IntegrationClient.getResponse(testRule, String.format("departures/%s/%s", lat, lon),
                Optional.empty());
        assertEquals(200,response.getStatus());

        DepartureListDTO departureList = response.readEntity(DepartureListDTO.class);

        SortedSet<DepartureDTO> departures = departureList.getDepartures();
        assertFalse(departures.isEmpty());
        DepartureDTO departureDTO = departures.first();
        TramTime when = departureDTO.getWhen();
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

    @Test
    @Category(LiveDataHealthCheck.class)
    public void shouldGetDueTramsForStation() {
        Response response = IntegrationClient.getResponse(
                testRule, String.format("departures/station/%s", Stations.StPetersSquare.getId()), Optional.empty());
        assertEquals(200,response.getStatus());

        DepartureListDTO departureList = response.readEntity(DepartureListDTO.class);
        SortedSet<DepartureDTO> departures = departureList.getDepartures();
        assertFalse(departures.isEmpty());
        assertFalse(departureList.getNotes().isEmpty());

        departures.forEach(depart -> {
            assertEquals(Stations.StPetersSquare.getName(),depart.getFrom());
        });
    }

    @Test
    @Category(LiveDataHealthCheck.class)
    public void shouldGetDueTramsForStationNotesOnOrOff() {
        Response response = IntegrationClient.getResponse(
                testRule, String.format("departures/station/%s?notes=1", Stations.StPetersSquare.getId()), Optional.empty());
        assertEquals(200,response.getStatus());

        DepartureListDTO departureList = response.readEntity(DepartureListDTO.class);
        assertFalse(departureList.getNotes().isEmpty());

        response = IntegrationClient.getResponse(
                testRule, String.format("departures/station/%s?notes=0", Stations.StPetersSquare.getId()), Optional.empty());
        assertEquals(200,response.getStatus());

        departureList = response.readEntity(DepartureListDTO.class);
        assertTrue(departureList.getNotes().isEmpty());

    }

}
