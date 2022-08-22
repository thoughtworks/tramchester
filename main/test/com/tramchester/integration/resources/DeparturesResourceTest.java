package com.tramchester.integration.resources;

import com.tramchester.App;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.LocationType;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.DeparturesQueryDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.APIClient;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.tram.ResourceTramTestConfig;
import com.tramchester.livedata.domain.DTO.DepartureDTO;
import com.tramchester.livedata.domain.DTO.DepartureListDTO;
import com.tramchester.livedata.domain.liveUpdates.PlatformMessage;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;
import com.tramchester.livedata.repository.PlatformMessageSource;
import com.tramchester.livedata.repository.UpcomingDeparturesSource;
import com.tramchester.livedata.tfgm.TramDepartureRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.resources.DeparturesResource;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.LiveDataMessagesCategory;
import com.tramchester.testSupport.testTags.LiveDataTestCategory;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static com.tramchester.testSupport.reference.KnownLocations.nearAltrinchamInterchange;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DropwizardExtensionsSupport.class)
class DeparturesResourceTest {

    private static final IntegrationAppExtension appExtension = new IntegrationAppExtension(App.class,
            new ResourceTramTestConfig<>(DeparturesResource.class, true));

    private Station stationWithNotes;
    private Station stationWithDepartures;

    @BeforeEach
    void beforeEachTestRuns() {
        final App app =  appExtension.getApplication();
        final GuiceContainerDependencies dependencies = app.getDependencies();

        PlatformMessageSource platformMessageSource = dependencies.get(PlatformMessageSource.class);
        StationRepository stationRepository = dependencies.get(StationRepository.class);
        ProvidesLocalNow providesLocalNow = dependencies.get(ProvidesLocalNow.class);

        TramDate queryDate = providesLocalNow.getTramDate();
        TramTime time = providesLocalNow.getNowHourMins();

        Optional<PlatformMessage> searchForMessage = stationRepository.getAllStationStream().
                map(station -> platformMessageSource.messagesFor(station, queryDate, time)).
                flatMap(Collection::stream).
                findAny();
        searchForMessage.ifPresent(platformMessage -> stationWithNotes = platformMessage.getStation());

        UpcomingDeparturesSource dueTramsSource = dependencies.get(TramDepartureRepository.class);

        Optional<UpcomingDeparture> searchForDueTrams = stationRepository.getAllStationStream().
                flatMap(station -> dueTramsSource.forStation(station).stream()).
                findAny();
        searchForDueTrams.ifPresent(dueTram -> stationWithDepartures = dueTram.getDisplayLocation());
    }

    @Test
    @LiveDataMessagesCategory
    void shouldHaveAStationWithAMessage() {
        assertNotNull(stationWithNotes, "No station with notes");
    }

    @Test
    @LiveDataTestCategory
    void shouldHaveAStationWithDepartures() {
        assertNotNull(stationWithDepartures);
    }

    @Test
    @LiveDataTestCategory
    void shouldGetDueTramsForStation() {
        // split out messages to own test as need to be able to disable those separately
        assertNotNull(stationWithDepartures, "No station with notes");
        Response response = getResponseForStation(stationWithDepartures, false);
        assertEquals(200, response.getStatus());
        DepartureListDTO departureList = response.readEntity(DepartureListDTO.class);

        SortedSet<DepartureDTO> departures = departureList.getDepartures();
        assertFalse(departures.isEmpty(), "no departures found for " + stationWithDepartures.getName());
        departures.forEach(depart -> assertEquals(stationWithDepartures.getName(), depart.getFrom()));
    }
    
    @Test
    @LiveDataMessagesCategory
    void shouldHaveMessagesForStation() {
        assertNotNull(stationWithNotes, "No station with notes");
        Response response = getResponseForStation(stationWithNotes, true);
        assertEquals(200, response.getStatus());
        DepartureListDTO departureList = response.readEntity(DepartureListDTO.class);

        assertFalse(departureList.getNotes().isEmpty(), "no notes found for " + stationWithNotes.getName());
    }

    @Test
    @LiveDataTestCategory
    void shouldGetDueTramsForStationWithQuerytimeNow() {
        Station station = stationWithDepartures;

        LocalTime now = TestEnv.LocalNow().toLocalTime();
        SortedSet<DepartureDTO> departures = getDeparturesForStationTime(station, now);
        assertFalse(departures.isEmpty(), "no due trams at " + station);
        departures.forEach(depart -> assertEquals(station.getName(), depart.getFrom()));
    }

    @Test
    @LiveDataTestCategory
    void shouldGetDueTramsForStationWithQuerytimePast() {
        LocalTime queryTime = TestEnv.LocalNow().toLocalTime().minusMinutes(120);

        SortedSet<DepartureDTO> departures = getDeparturesForStationTime(stationWithDepartures, queryTime);
        assertTrue(departures.isEmpty());
    }

    @Test
    @LiveDataTestCategory
    void shouldGetDueTramsForStationWithQuerytimeFuture() {
        LocalTime queryTime = TestEnv.LocalNow().toLocalTime().plusMinutes(120);

        SortedSet<DepartureDTO> departures = getDeparturesForStationTime(stationWithDepartures, queryTime);

        assertTrue(departures.isEmpty());
    }


    @Test
    @LiveDataTestCategory
    void shouldGetNearbyDeparturesQuerytimeNow() {
        LatLong where = nearAltrinchamInterchange.latLong();
        LocalTime queryTime = TestEnv.LocalNow().toLocalTime();
        SortedSet<DepartureDTO> departures = getDeparturesForLatlongTime(where, queryTime);
        assertFalse(departures.isEmpty(), "no departures for lat/long altrincham");
    }

    @Test
    @LiveDataTestCategory
    void shouldGetNearbyDeparturesQuerytimeFuture() {
        LatLong latLong = new LatLong(53.4804263d, -2.2392436d);
        LocalTime queryTime = TestEnv.LocalNow().toLocalTime().plusMinutes(120);
        SortedSet<DepartureDTO> departures = getDeparturesForLatlongTime(latLong, queryTime);
        assertTrue(departures.isEmpty());
    }

    @Test
    @LiveDataTestCategory
    void shouldGetNearbyDeparturesQuerytimePast() {
        LatLong latLong = new LatLong(53.4804263d, -2.2392436d);
        LocalTime queryTime = TestEnv.LocalNow().toLocalTime().minusMinutes(120);

        SortedSet<DepartureDTO> departures = getDeparturesForLatlongTime(latLong, queryTime);

        assertTrue(departures.isEmpty());
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

        TramTime nowMinus5mins = TramTime.ofHourMins(TestEnv.LocalNow().toLocalTime().minusMinutes(6));
        assertTrue(when.toLocalTime().isAfter(nowMinus5mins.asLocalTime()), when.toString());

        String nextDepart = departureDTO.getFrom();
        assertTrue(nearAlty.contains(nextDepart), nextDepart);
        assertFalse(departureDTO.getStatus().isEmpty());
        assertFalse(departureDTO.getDestination().isEmpty());

    }

    @Test
    @LiveDataMessagesCategory
    void shouldGetDueTramsForStationNotesRequestedOrNot() {
        Station station = stationWithNotes;

        assertNotNull(stationWithNotes, "No station with notes");
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

    private SortedSet<DepartureDTO> getDeparturesForLatlongTime(LatLong where, LocalTime queryTime) {

        DeparturesQueryDTO queryDTO = getQueryDTO(where);
        queryDTO.setTime(queryTime);

        Response response = getPostResponse(queryDTO);

        DepartureListDTO departureList = response.readEntity(DepartureListDTO.class);
        return departureList.getDepartures();
    }

    private SortedSet<DepartureDTO> getDeparturesForStationTime(Station station, LocalTime queryTime) {
        DeparturesQueryDTO queryDTO = getQueryDTO(station, false);
        queryDTO.setTime(queryTime);

        Response response = getPostResponse(queryDTO);

        DepartureListDTO departureList = response.readEntity(DepartureListDTO.class);
        return departureList.getDepartures();
    }

    private Response getResponseForStation(Station station, boolean includeNotes) {
        DeparturesQueryDTO queryDTO = getQueryDTO(station, includeNotes);
        return getPostResponse(queryDTO);
    }

    private Response getResponseForLocation(LatLong where) {
        DeparturesQueryDTO queryDTO = getQueryDTO(where);
        return getPostResponse(queryDTO);
    }

    @NotNull
    private Response getPostResponse(DeparturesQueryDTO queryDTO) {
        Response response = APIClient.postAPIRequest(appExtension, "departures/location", queryDTO);
        assertEquals(200, response.getStatus());
        return response;
    }

    @NotNull
    private DeparturesQueryDTO getQueryDTO(Station station, boolean includeNotes) {
        return new DeparturesQueryDTO(LocationType.Station, station.getId().forDTO(),
                includeNotes);
    }

    @NotNull
    private DeparturesQueryDTO getQueryDTO(LatLong where) {
        String latLong = String.format("%s,%s", where.getLat(), where.getLon());
        return new DeparturesQueryDTO(LocationType.MyLocation, latLong, false);
    }


}
