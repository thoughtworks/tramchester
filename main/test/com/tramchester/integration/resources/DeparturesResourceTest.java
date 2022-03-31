package com.tramchester.integration.resources;

import com.tramchester.App;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.livedata.LiveDataUpdaterTest;
import com.tramchester.integration.testSupport.APIClient;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.tram.ResourceTramTestConfig;
import com.tramchester.livedata.domain.DTO.DepartureDTO;
import com.tramchester.livedata.domain.DTO.DepartureListDTO;
import com.tramchester.resources.DeparturesResource;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.LiveDataMessagesCategory;
import com.tramchester.testSupport.testTags.LiveDataTestCategory;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;

import static com.tramchester.testSupport.reference.KnownLocations.nearAltrinchamInterchange;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DropwizardExtensionsSupport.class)
class DeparturesResourceTest {

    private static final IntegrationAppExtension appExtension = new IntegrationAppExtension(App.class,
            new ResourceTramTestConfig<>(DeparturesResource.class, true));

    private final TramStations stationWithNotes = LiveDataUpdaterTest.StationWithNotes;

    @Test
    @LiveDataTestCategory
    void shouldGetDueTramsForStation() {
        // split out messages to own test as need to be able to disable those separately
        Response response = getResponseForStation(stationWithNotes, false);
        assertEquals(200, response.getStatus());
        DepartureListDTO departureList = response.readEntity(DepartureListDTO.class);

        SortedSet<DepartureDTO> departures = departureList.getDepartures();
        assertFalse(departures.isEmpty(), "no departures found for " + stationWithNotes.getName());
        departures.forEach(depart -> assertEquals(stationWithNotes.getName(), depart.getFrom()));
    }
    
    @Test
    @LiveDataMessagesCategory
    void shouldHaveMessagesForStation() {
        Response response = getResponseForStation(stationWithNotes, false);
        assertEquals(200, response.getStatus());
        DepartureListDTO departureList = response.readEntity(DepartureListDTO.class);

        // off by deafult
        assertFalse(departureList.getNotes().isEmpty(), "no notes found for " + stationWithNotes.getName());
    }

    @Test
    @LiveDataTestCategory
    void shouldGetDueTramsForStationWithQuerytimeNow() {
        LocalTime queryTime = TestEnv.LocalNow().toLocalTime();
        TramStations station = TramStations.ManAirport;

        SortedSet<DepartureDTO> departures = getDeparturesForStationTime(queryTime, station);
        assertFalse(departures.isEmpty(), "no due trams at " + station);
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


    @Test
    @LiveDataTestCategory
    void shouldGetNearbyDeparturesQuerytimeNow() {
        LatLong where = nearAltrinchamInterchange.latLong();
        LocalTime queryTime = TestEnv.LocalNow().toLocalTime();
        SortedSet<DepartureDTO> departures = getDeparturesForLatlongTime(where.getLat(), where.getLon(), queryTime);
        assertFalse(departures.isEmpty(), "no departures for lat/long altrincham");
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
        Response response = getResponseForLocationAndTime(lat, lon, time);
        assertEquals(500, response.getStatus());
    }
    
    @Test
    @LiveDataTestCategory
    void shouldNotGetNearbyIfOutsideOfThreshold() {
        LatLong where = nearAltrinchamInterchange.latLong();

        final List<String> nearAlty = Arrays.asList(TramStations.Altrincham.getName(),
                TramStations.NavigationRoad.getName());

        Response response = getResponseForLocation(where);
        assertEquals(200, response.getStatus());

        DepartureListDTO departureList = response.readEntity(DepartureListDTO.class);

        SortedSet<DepartureDTO> departures = departureList.getDepartures();
        assertFalse(departures.isEmpty(), "no departures");

        DepartureDTO departureDTO = departures.first();
        LocalDateTime when = departureDTO.getDueTime();

        TramTime nowMinus5mins = TramTime.of(TestEnv.LocalNow().toLocalTime().minusMinutes(6));
        assertTrue(when.toLocalTime().isAfter(nowMinus5mins.asLocalTime()), when.toString());

        String nextDepart = departureDTO.getFrom();
        assertTrue(nearAlty.contains(nextDepart), nextDepart);
        assertFalse(departureDTO.getStatus().isEmpty());
        assertFalse(departureDTO.getDestination().isEmpty());

    }

    @Test
    @LiveDataMessagesCategory
    void shouldGetDueTramsForStationNotesRequestedOrNot() {
        TramStations station = stationWithNotes;

        // Notes disabled
        Response response = getResponseForStation(station, false);
        assertEquals(200, response.getStatus());

        DepartureListDTO departureList = response.readEntity(DepartureListDTO.class);
        assertTrue(departureList.getNotes().isEmpty(), "no notes expected");

        // Notes enabled
        response = getResponseForStation(station, true);
        assertEquals(200, response.getStatus());

        departureList = response.readEntity(DepartureListDTO.class);
        assertFalse(departureList.getNotes().isEmpty(), "no notes for " + station);

    }

    private String getStationQueryURL(TramStations station, boolean includeNotes) {
        String notes = includeNotes? "1" :"0";
        return String.format("departures/station/%s?notes=%s", station.getRawId(), notes);
    }

    private Response getResponseForStation(TramStations station, boolean includeNotes) {
        return APIClient.getApiResponse(appExtension, getStationQueryURL(station, includeNotes));
    }

    private Response getResponseForLocationAndTime(double lat, double lon, String time) {
        return APIClient.getApiResponse(appExtension, String.format("departures/%s/%s?querytime=%s", lat, lon, time));
    }

    private Response getResponseForLocation(LatLong where) {
        return APIClient.getApiResponse(appExtension, String.format("departures/%s/%s", where.getLat(), where.getLon()));
    }

    private SortedSet<DepartureDTO> getDeparturesForLatlongTime(double lat, double lon, LocalTime queryTime) {
        String time = queryTime.format(TestEnv.timeFormatter);
        //Response response = APIClient.getApiResponse(appExtension, String.format("departures/%s/%s?querytime=%s", lat, lon, time));
        Response response = getResponseForLocationAndTime(lat, lon, time);
        assertEquals(200, response.getStatus());
        DepartureListDTO departureList = response.readEntity(DepartureListDTO.class);
        return departureList.getDepartures();
    }
    
    private SortedSet<DepartureDTO> getDeparturesForStationTime(LocalTime queryTime, TramStations station) {
        String time = queryTime.format(TestEnv.timeFormatter);
        Response response = APIClient.getApiResponse(appExtension, String.format("departures/station/%s?querytime=%s", station.getRawId(), time));
        assertEquals(200, response.getStatus());

        DepartureListDTO departureList = response.readEntity(DepartureListDTO.class);
        return departureList.getDepartures();
    }

}
