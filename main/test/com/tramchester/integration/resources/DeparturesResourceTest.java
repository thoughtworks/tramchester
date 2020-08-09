package com.tramchester.integration.resources;

import com.tramchester.App;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.DepartureDTO;
import com.tramchester.domain.presentation.DTO.DepartureListDTO;
import com.tramchester.domain.presentation.Note;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.IntegrationClient;
import com.tramchester.integration.IntegrationAppExtension;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.testSupport.*;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.core.Response;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DropwizardExtensionsSupport.class)
class DeparturesResourceTest {

    private static final IntegrationAppExtension appExtension = new IntegrationAppExtension(App.class, new IntegrationTramTestConfig());

    private final List<String> nearby = Arrays.asList(Stations.PiccadillyGardens.getName(),
            Stations.StPetersSquare.getName(),
            Stations.Piccadilly.getName(),
            Stations.MarketStreet.getName(),
            Stations.ExchangeSquare.getName(),
            "Shudehill");

    @Test
    @LiveDataTestCategory
    void shouldGetDueTramsForStation() {
        Station station = Stations.StPetersSquare;

        Response response = IntegrationClient.getApiResponse(
                appExtension, String.format("departures/station/%s", station.forDTO()));
        assertEquals(200, response.getStatus());
        DepartureListDTO departureList = response.readEntity(DepartureListDTO.class);

        SortedSet<DepartureDTO> departures = departureList.getDepartures();
        assertFalse(departures.isEmpty());
        departures.forEach(depart -> assertEquals(station.getName(),depart.getFrom()));
        assertFalse(departureList.getNotes().isEmpty()); // off by deafult
    }

    @Test
    @LiveDataTestCategory
    void shouldGetDueTramsForStationWithQuerytimeNow() {
        LocalTime queryTime = TestEnv.LocalNow().toLocalTime();
        Station station = Stations.StPetersSquare;

        SortedSet<DepartureDTO> departures = getDeparturesForStationTime(queryTime, station);
        assertFalse(departures.isEmpty(), "no due trams");
        departures.forEach(depart -> assertEquals(station.getName(),depart.getFrom()));
    }

    @Test
    @LiveDataTestCategory
    void shouldGetDueTramsForStationWithQuerytimePast() {
        LocalTime queryTime = TestEnv.LocalNow().toLocalTime().minusMinutes(120);
        Station station = Stations.StPetersSquare;

        SortedSet<DepartureDTO> departures = getDeparturesForStationTime(queryTime, station);
        assertTrue(departures.isEmpty());
    }

    @Test
    @LiveDataTestCategory
    void shouldGetDueTramsForStationWithQuerytimeFuture() {
        LocalTime queryTime = TestEnv.LocalNow().toLocalTime().plusMinutes(120);
        Station station = Stations.StPetersSquare;

        SortedSet<DepartureDTO> departures = getDeparturesForStationTime(queryTime, station);

        assertTrue(departures.isEmpty());
    }

    private SortedSet<DepartureDTO> getDeparturesForStationTime(LocalTime queryTime, Station station) {
        String time = queryTime.format(TestEnv.timeFormatter);
        Response response = IntegrationClient.getApiResponse(
                appExtension, String.format("departures/station/%s?querytime=%s", station.forDTO(), time));
        assertEquals(200, response.getStatus());

        DepartureListDTO departureList = response.readEntity(DepartureListDTO.class);
        return departureList.getDepartures();
    }

    @Test
    @LiveDataMessagesCategory
    void shouldGetNearbyDeparturesQuerytimeNow() {
        double lat = 53.4804263d;
        double lon = -2.2392436d;
        LocalTime queryTime = TestEnv.LocalNow().toLocalTime();
        SortedSet<DepartureDTO> departures = getDeparturesForLatlongTime(lat, lon, queryTime);
        assertFalse(departures.isEmpty());
    }

    @Test
    @LiveDataMessagesCategory
    void shouldGetNearbyDeparturesQuerytimeFuture() {
        double lat = 53.4804263d;
        double lon = -2.2392436d;
        LocalTime queryTime = TestEnv.LocalNow().toLocalTime().plusMinutes(120);
        SortedSet<DepartureDTO> departures = getDeparturesForLatlongTime(lat, lon, queryTime);
        assertTrue(departures.isEmpty());
    }

    @Test
    @LiveDataMessagesCategory
    void shouldGetNearbyDeparturesQuerytimePast() {
        double lat = 53.4804263d;
        double lon = -2.2392436d;
        LocalTime queryTime = TestEnv.LocalNow().toLocalTime().minusMinutes(120);

        SortedSet<DepartureDTO> departures = getDeparturesForLatlongTime(lat, lon, queryTime);

        assertTrue(departures.isEmpty());
    }

    private SortedSet<DepartureDTO> getDeparturesForLatlongTime(double lat, double lon, LocalTime queryTime) {
        String time = queryTime.format(TestEnv.timeFormatter);
        Response response = IntegrationClient.getApiResponse(appExtension, String.format("departures/%s/%s?querytime=%s", lat, lon, time));
        assertEquals(200, response.getStatus());
        DepartureListDTO departureList = response.readEntity(DepartureListDTO.class);
        return departureList.getDepartures();
    }

    @Test
    @LiveDataMessagesCategory
    void shouldNotGetNearIfOutsideOfThreshold() {
        double lat = 53.4804263d;
        double lon = -2.2392436d;

        Response response = IntegrationClient.getApiResponse(appExtension, String.format("departures/%s/%s", lat, lon));
        assertEquals(200, response.getStatus());

        DepartureListDTO departureList = response.readEntity(DepartureListDTO.class);

        SortedSet<DepartureDTO> departures = departureList.getDepartures();
        assertFalse(departures.isEmpty());

        DepartureDTO departureDTO = departures.first();
        TramTime when = departureDTO.getWhen();

        TramTime nowWithin5mins = TramTime.of(TestEnv.LocalNow().toLocalTime().minusMinutes(5));
        assertTrue(when.asLocalTime().isAfter(nowWithin5mins.asLocalTime()) );

        String nextDepart = departureDTO.getFrom();
        assertTrue(nearby.contains(nextDepart), nextDepart);
        assertFalse(departureDTO.getStatus().isEmpty());
        assertFalse(departureDTO.getDestination().isEmpty());

        List<Note> notes = departureList.getNotes();
        assertFalse(notes.isEmpty());
        // ignore closure message which is always present, also if today is weekend exclude that
        int ignore = 1;
        DayOfWeek dayOfWeek = TestEnv.LocalNow().toLocalDate().getDayOfWeek();
        if (dayOfWeek.equals(DayOfWeek.SATURDAY) || dayOfWeek.equals(DayOfWeek.SUNDAY)) {
            ignore++;
        }
        assertTrue((notes.size())-ignore>0);
    }

    @Test
    @LiveDataMessagesCategory
    void shouldGetDueTramsForStationNotesOnOrOff() {
        Response response = IntegrationClient.getApiResponse(
                appExtension, String.format("departures/station/%s?notes=1", Stations.StPetersSquare.forDTO()));
        assertEquals(200, response.getStatus());

        DepartureListDTO departureList = response.readEntity(DepartureListDTO.class);
        assertFalse(departureList.getNotes().isEmpty());

        response = IntegrationClient.getApiResponse(
                appExtension, String.format("departures/station/%s?notes=0", Stations.StPetersSquare.forDTO()));
        assertEquals(200, response.getStatus());

        departureList = response.readEntity(DepartureListDTO.class);
        assertTrue(departureList.getNotes().isEmpty());

    }

}
