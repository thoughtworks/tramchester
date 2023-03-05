package com.tramchester.integration.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.App;
import com.tramchester.domain.Timestamped;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.places.LocationType;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.LocationDTO;
import com.tramchester.domain.presentation.DTO.LocationRefDTO;
import com.tramchester.domain.presentation.DTO.PlatformDTO;
import com.tramchester.domain.presentation.DTO.RouteRefDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.RecentJourneys;
import com.tramchester.integration.testSupport.APIClient;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.tram.ResourceTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.resources.StationResource;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.reference.KnownLocations.nearPiccGardens;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DropwizardExtensionsSupport.class)
class StationResourceTest {

    private static final IntegrationAppExtension appExtension =
            new IntegrationAppExtension(App.class, new ResourceTramTestConfig<>(StationResource.class));

    private final ObjectMapper mapper = new ObjectMapper();
    private StationRepository stationRepository;

    @BeforeEach
    void beforeEachTestRuns() {
        App app =  appExtension.getApplication();
        stationRepository = app.getDependencies().get(StationRepository.class);
    }

    @Test
    void shouldGetSingleStationWithPlatforms() {
        String stationId = TramStations.StPetersSquare.getRawId();
        String endPoint = "stations/" + stationId;
        Response response = APIClient.getApiResponse(appExtension, endPoint);
        Assertions.assertEquals(200,response.getStatus());
        LocationDTO result = response.readEntity(LocationDTO.class);

        Assertions.assertEquals(stationId, result.getId().getActualId());

        List<PlatformDTO> platforms = result.getPlatforms();
        Assertions.assertEquals(4, platforms.size());
        List<String> platformIds = platforms.stream().
                map(PlatformDTO::getId).
                map(IdForDTO::getActualId).
                collect(Collectors.toList());

        Assertions.assertTrue(platformIds.contains(stationId+"1"));
        Assertions.assertTrue(platformIds.contains(stationId+"2"));
        Assertions.assertTrue(platformIds.contains(stationId+"3"));
        Assertions.assertTrue(platformIds.contains(stationId+"4"));

        List<RouteRefDTO> routeRefDTOS = result.getRoutes();

        assertFalse(routeRefDTOS.isEmpty());

        Station station = stationRepository.getStationById(TramStations.StPetersSquare.getId());
        int stationRoutesNumber = station.getPickupRoutes().size() + station.getDropoffRoutes().size();

        assertEquals(routeRefDTOS.size(),stationRoutesNumber);

    }

    @Test
    void shouldGetTramStations() {
        Response result = APIClient.getApiResponse(appExtension, "stations/mode/Tram");

        assertEquals(200, result.getStatus());

        List<LocationRefDTO> results = result.readEntity(new GenericType<>() {});

        Set<String> expectedIds = stationRepository.getStations().stream().
                map(station -> station.getId().forDTO()).collect(Collectors.toSet());

        assertEquals(expectedIds.size(), results.size());

        List<String> resultIds = results.stream().
                map(LocationRefDTO::getId).
                map(IdForDTO::getActualId).
                collect(Collectors.toList());
        assertTrue(expectedIds.containsAll(resultIds));

        ArrayList<LocationRefDTO> sortedResults = new ArrayList<>(results);
        sortedResults.sort(Comparator.comparing(item -> item.getName().toLowerCase()));

        for (int i = 0; i < sortedResults.size(); i++) {
            assertEquals(results.get(i), sortedResults.get(i), "not sorted");
        }

    }

    @Test
    void shouldGetTramStation304response() {
        Response resultA = APIClient.getApiResponse(appExtension, "stations/mode/Tram");
        assertEquals(200, resultA.getStatus());

        Date lastMod = resultA.getLastModified();

        Response resultB = APIClient.getApiResponse(appExtension, "stations/mode/Tram", lastMod);
        assertEquals(304, resultB.getStatus());
    }

    @Test
    void shouldGetAllStationsWithDetails() {
        Response response = APIClient.getApiResponse(appExtension, "stations/all");
        assertEquals(200, response.getStatus());

        List<LocationDTO> results = response.readEntity(new GenericType<>() {});

        assertEquals(stationRepository.getStations().size(), results.size());

        Station stPeters = TramStations.StPetersSquare.from(stationRepository);
        IdForDTO expected = new IdForDTO(stPeters.getId());

        Optional<LocationDTO> found = results.stream().
                filter(item -> item.getId().equals(expected)).findFirst();

        assertTrue(found.isPresent());

        LocationDTO result = found.get();

        assertEquals(LocationType.Station, result.getLocationType());
        assertEquals(stPeters.getPlatforms().size(), result.getPlatforms().size());

        // WIP
        //assertTrue(result.getIsInterchange());
    }

    @Test
    void shouldGetBusStations() {
        Response result = APIClient.getApiResponse(appExtension, "stations/mode/Bus");

        assertEquals(200, result.getStatus());

        // buses disabled, but should still get a list back, albeit empty
        List<LocationRefDTO> results = result.readEntity(new GenericType<>() {});
        assertEquals(0, results.size());
    }

    @Test
    void should404ForUnknownMode() {
        Response result = APIClient.getApiResponse(appExtension, "stations/mode/Jumping");
        assertEquals(404, result.getStatus());
    }

    @Test
    void shouldGetNearestStations() {

        LatLong place = nearPiccGardens.latLong();
        Response result = APIClient.getApiResponse(appExtension, String.format("stations/near?lat=%s&lon=%s",
                place.getLat(), place.getLon()));
        assertEquals(200, result.getStatus());

        List<LocationRefDTO> stationList = result.readEntity(new GenericType<>() {});

        assertEquals(5,stationList.size());
        Set<String> ids = stationList.stream().
                map(LocationRefDTO::getId).
                map(IdForDTO::getActualId).
                collect(Collectors.toSet());

        assertTrue(ids.contains(TramStations.PiccadillyGardens.getRawId()));
        //assertTrue(ids.contains(TramStations.Piccadilly.forDTO()));
        assertTrue(ids.contains(TramStations.StPetersSquare.getRawId()));
        assertTrue(ids.contains(TramStations.MarketStreet.getRawId()));
        assertTrue(ids.contains(TramStations.ExchangeSquare.getRawId()));
        assertTrue(ids.contains(TramStations.Shudehill.getRawId()));
    }

    @Test
    void shouldGetRecentStations() throws JsonProcessingException {
        Cookie cookie = createRecentsCookieFor(TramStations.Altrincham, TramStations.Bury, TramStations.ManAirport);

        // All
        Response result = APIClient.getApiResponse(appExtension, "stations/recent", List.of(cookie));
        assertEquals(200, result.getStatus());

        List<LocationRefDTO> stationDtos = result.readEntity(new GenericType<>() {});

        assertEquals(3, stationDtos.size());

        Set<String> ids = stationDtos.stream().
                map(LocationRefDTO::getId).
                map(IdForDTO::getActualId).
                collect(Collectors.toSet());

        assertTrue(ids.contains(TramStations.Altrincham.getRawId()));
        assertTrue(ids.contains(TramStations.Bury.getRawId()));
        assertTrue(ids.contains(TramStations.ManAirport.getRawId()));

    }

    @NotNull
    private Cookie createRecentsCookieFor(TramStations... stations) throws JsonProcessingException {
        RecentJourneys recentJourneys = new RecentJourneys();

        Set<Timestamped> recents = new HashSet<>();
        for (TramStations station : stations) {
            Timestamped timestamped = new Timestamped(station.getId(), TestEnv.LocalNow());
            recents.add(timestamped);
        }
        recentJourneys.setTimestamps(recents);

        String recentAsString = RecentJourneys.encodeCookie(mapper,recentJourneys);
        return new Cookie("tramchesterRecent", recentAsString);
    }

}
