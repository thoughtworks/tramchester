package com.tramchester.integration.resources;

import com.tramchester.App;
import com.tramchester.domain.presentation.DTO.DepartureDTO;
import com.tramchester.domain.presentation.DTO.DepartureListDTO;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.IntegrationClient;
import com.tramchester.integration.testSupport.IntegrationTramTestConfig;
import com.tramchester.testSupport.LiveDataMessagesCategory;
import com.tramchester.testSupport.LiveDataTestCategory;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.integration.livedata.LiveDataUpdaterTest;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DropwizardExtensionsSupport.class)
class DeparturesResourceTest {

    private static final IntegrationAppExtension appExtension = new IntegrationAppExtension(App.class, new IntegrationTramTestConfig());

    private final List<String> nearby = Arrays.asList(TramStations.PiccadillyGardens.getName(),
            TramStations.StPetersSquare.getName(),
            TramStations.Piccadilly.getName(),
            TramStations.MarketStreet.getName(),
            TramStations.ExchangeSquare.getName(),
            "Shudehill");

    private final TramStations stationWithNotes = LiveDataUpdaterTest.StationWithNotes;

    @Test
    @LiveDataTestCategory
    void shouldGetDueTramsForStation() {
        // split out messages to own test as need to be able to disable those seperately
        Response response = IntegrationClient.getApiResponse(
                appExtension, String.format("departures/station/%s", stationWithNotes.forDTO()));
        assertEquals(200, response.getStatus());
        DepartureListDTO departureList = response.readEntity(DepartureListDTO.class);

        SortedSet<DepartureDTO> departures = departureList.getDepartures();
        assertFalse(departures.isEmpty(), "no departures found for " + stationWithNotes.getName());
        departures.forEach(depart -> assertEquals(stationWithNotes.getName(), depart.getFrom()));
    }

    @Test
    @LiveDataMessagesCategory
    void shouldMessagesForStation() {
        Response response = IntegrationClient.getApiResponse(
                appExtension, String.format("departures/station/%s", stationWithNotes.forDTO()));
        assertEquals(200, response.getStatus());
        DepartureListDTO departureList = response.readEntity(DepartureListDTO.class);

        assertFalse(departureList.getNotes().isEmpty(), "no notes found for " + stationWithNotes.getName()); // off by deafult
    }

    @Test
    @LiveDataTestCategory
    void shouldGetDueTramsForStationWithQuerytimeNow() {
        LocalTime queryTime = TestEnv.LocalNow().toLocalTime();
        TramStations station = TramStations.StPetersSquare;

        SortedSet<DepartureDTO> departures = getDeparturesForStationTime(queryTime, station);
        assertFalse(departures.isEmpty(), "no due trams");
        departures.forEach(depart -> assertEquals(station.getName(),depart.getFrom()));
    }

    @Test
    @LiveDataTestCategory
    void shouldGetDueTramsForStationWithQuerytimePast() {
        LocalTime queryTime = TestEnv.LocalNow().toLocalTime().minusMinutes(120);
        TramStations station = TramStations.StPetersSquare;

        SortedSet<DepartureDTO> departures = getDeparturesForStationTime(queryTime, station);
        assertTrue(departures.isEmpty());
    }

    @Test
    @LiveDataTestCategory
    void shouldGetDueTramsForStationWithQuerytimeFuture() {
        LocalTime queryTime = TestEnv.LocalNow().toLocalTime().plusMinutes(120);
        TramStations station = TramStations.StPetersSquare;

        SortedSet<DepartureDTO> departures = getDeparturesForStationTime(queryTime, station);

        assertTrue(departures.isEmpty());
    }

    private SortedSet<DepartureDTO> getDeparturesForStationTime(LocalTime queryTime, TramStations station) {
        String time = queryTime.format(TestEnv.timeFormatter);
        Response response = IntegrationClient.getApiResponse(
                appExtension, String.format("departures/station/%s?querytime=%s", station.forDTO(), time));
        assertEquals(200, response.getStatus());

        DepartureListDTO departureList = response.readEntity(DepartureListDTO.class);
        return departureList.getDepartures();
    }

    @Test
    @LiveDataTestCategory
    void shouldGetNearbyDeparturesQuerytimeNow() {
        double lat = 53.4804263d;
        double lon = -2.2392436d;
        LocalTime queryTime = TestEnv.LocalNow().toLocalTime();
        SortedSet<DepartureDTO> departures = getDeparturesForLatlongTime(lat, lon, queryTime);
        assertFalse(departures.isEmpty(), "no departures");
    }

    @Test
    @LiveDataTestCategory
    void shouldGetNearbyDeparturesQuerytimeFuture() {
        double lat = 53.4804263d;
        double lon = -2.2392436d;
        LocalTime queryTime = TestEnv.LocalNow().toLocalTime().plusMinutes(120);
        SortedSet<DepartureDTO> departures = getDeparturesForLatlongTime(lat, lon, queryTime);
        assertTrue(departures.isEmpty());
    }

    @Test
    @LiveDataTestCategory
    void shouldGetNearbyDeparturesQuerytimePast() {
        double lat = 53.4804263d;
        double lon = -2.2392436d;
        LocalTime queryTime = TestEnv.LocalNow().toLocalTime().minusMinutes(120);

        SortedSet<DepartureDTO> departures = getDeparturesForLatlongTime(lat, lon, queryTime);

        assertTrue(departures.isEmpty());
    }

    @Test
    void shouldGetServerErrorForInvalidTime() {
        double lat = 53.4804263d;
        double lon = -2.2392436d;
        String time = "28:64";
        Response response = IntegrationClient.getApiResponse(appExtension, String.format("departures/%s/%s?querytime=%s", lat, lon, time));
        assertEquals(500, response.getStatus());
    }

    private SortedSet<DepartureDTO> getDeparturesForLatlongTime(double lat, double lon, LocalTime queryTime) {
        String time = queryTime.format(TestEnv.timeFormatter);
        Response response = IntegrationClient.getApiResponse(appExtension, String.format("departures/%s/%s?querytime=%s", lat, lon, time));
        assertEquals(200, response.getStatus());
        DepartureListDTO departureList = response.readEntity(DepartureListDTO.class);
        return departureList.getDepartures();
    }

    @Test
    @LiveDataTestCategory
    void shouldNotGetNearbyIfOutsideOfThreshold() {
        double lat = 53.4804263d;
        double lon = -2.2392436d;

        Response response = IntegrationClient.getApiResponse(appExtension, String.format("departures/%s/%s", lat, lon));
        assertEquals(200, response.getStatus());

        DepartureListDTO departureList = response.readEntity(DepartureListDTO.class);

        SortedSet<DepartureDTO> departures = departureList.getDepartures();
        assertFalse(departures.isEmpty(), "no departures");

        DepartureDTO departureDTO = departures.first();
        LocalDateTime when = departureDTO.getDueTime();

        TramTime nowWithin5mins = TramTime.of(TestEnv.LocalNow().toLocalTime().minusMinutes(5));
        assertTrue(when.toLocalTime().isAfter(nowWithin5mins.asLocalTime()) );

        String nextDepart = departureDTO.getFrom();
        assertTrue(nearby.contains(nextDepart), nextDepart);
        assertFalse(departureDTO.getStatus().isEmpty());
        assertFalse(departureDTO.getDestination().isEmpty());

    }

    @Test
    @LiveDataMessagesCategory
    void shouldGetDueTramsForStationNotesRequestedOrNot() {
        TramStations station = stationWithNotes;

        // Notes disabled
        Response response = IntegrationClient.getApiResponse(
                appExtension, String.format("departures/station/%s?notes=0", station.forDTO()));
        assertEquals(200, response.getStatus());

        DepartureListDTO departureList = response.readEntity(DepartureListDTO.class);
        assertTrue(departureList.getNotes().isEmpty(), "no notes expected");

        // Notes enabled
        response = IntegrationClient.getApiResponse(
                appExtension, String.format("departures/station/%s?notes=1", station.forDTO()));
        assertEquals(200, response.getStatus());

        departureList = response.readEntity(DepartureListDTO.class);
        assertFalse(departureList.getNotes().isEmpty(), "no notes for " + station);

    }

}
