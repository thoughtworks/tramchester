package com.tramchester.integration.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.App;
import com.tramchester.domain.Route;
import com.tramchester.domain.Timestamped;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.*;
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

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DropwizardExtensionsSupport.class)
class StationResourceTest {

    private static final IntegrationAppExtension appExtension =
            new IntegrationAppExtension(App.class, new ResourceTramTestConfig<>(StationResource.class));

    private final ObjectMapper mapper = new ObjectMapper();
    private StationRepository stationRepo;

    @BeforeEach
    void beforeEachTestRuns() {
        App app =  appExtension.getApplication();
        stationRepo = app.getDependencies().get(StationRepository.class);
    }

    @Test
    void shouldGetSingleStationWithPlatforms() {
        String stationId = TramStations.StPetersSquare.forDTO();
        String endPoint = "stations/" + stationId;
        Response response = APIClient.getApiResponse(appExtension, endPoint);
        Assertions.assertEquals(200,response.getStatus());
        LocationDTO result = response.readEntity(LocationDTO.class);

        Assertions.assertEquals(stationId, result.getId());

        List<PlatformDTO> platforms = result.getPlatforms();
        Assertions.assertEquals(4, platforms.size());
        List<String> platformIds = platforms.stream().map(PlatformDTO::getId).collect(Collectors.toList());
        Assertions.assertTrue(platformIds.contains(stationId+"1"));
        Assertions.assertTrue(platformIds.contains(stationId+"2"));
        Assertions.assertTrue(platformIds.contains(stationId+"3"));
        Assertions.assertTrue(platformIds.contains(stationId+"4"));

        List<RouteRefDTO> routeRefDTOS = result.getRoutes();

        assertFalse(routeRefDTOS.isEmpty());

        Station station = stationRepo.getStationById(TramStations.StPetersSquare.getId());
        Set<Route> stationRoutes = station.getRoutes();

        assertEquals(routeRefDTOS.size(), stationRoutes.size());

    }

    @Test
    void shouldGetTramStations() {
        Response result = APIClient.getApiResponse(appExtension, "stations/mode/Tram");

        assertEquals(200, result.getStatus());

        List<StationRefDTO> results = result.readEntity(new GenericType<>() {});

        Set<String> expectedIds = stationRepo.getStations().stream().map(station -> station.getId().forDTO()).collect(Collectors.toSet());
        assertEquals(expectedIds.size(), results.size());

        List<String> resultIds = results.stream().map(StationRefDTO::getId).collect(Collectors.toList());
        assertTrue(expectedIds.containsAll(resultIds));

        ArrayList<StationRefDTO> sortedResults = new ArrayList<>(results);
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
    void shouldGetBusStations() {
        Response result = APIClient.getApiResponse(appExtension, "stations/mode/Bus");

        assertEquals(200, result.getStatus());

        // buses disabled, but should still get a list back, albeit empty
        List<StationRefDTO> results = result.readEntity(new GenericType<>() {});
        assertEquals(0, results.size());
    }

    @Test
    void should404ForUnknownMode() {
        Response result = APIClient.getApiResponse(appExtension, "stations/mode/Jumping");
        assertEquals(404, result.getStatus());
    }

    @Test
    void shouldGetNearestStations() {

        LatLong nearPiccGardens = TestEnv.nearPiccGardens;
        Response result = APIClient.getApiResponse(appExtension, String.format("stations/near?lat=%s&lon=%s",
                nearPiccGardens.getLat(), nearPiccGardens.getLon()));
        assertEquals(200, result.getStatus());

        List<StationRefDTO> stationList = result.readEntity(new GenericType<>() {});

        assertEquals(5,stationList.size());
        Set<String> ids = stationList.stream().map(StationRefDTO::getId).collect(Collectors.toSet());
        assertTrue(ids.contains(TramStations.PiccadillyGardens.forDTO()));
        //assertTrue(ids.contains(TramStations.Piccadilly.forDTO()));
        assertTrue(ids.contains(TramStations.StPetersSquare.forDTO()));
        assertTrue(ids.contains(TramStations.MarketStreet.forDTO()));
        assertTrue(ids.contains(TramStations.ExchangeSquare.forDTO()));
        assertTrue(ids.contains(TramStations.Shudehill.forDTO()));
    }

    @Test
    void shouldGetRecentStations() throws JsonProcessingException {
        Cookie cookie = createRecentsCookieFor(TramStations.Altrincham, TramStations.Bury, TramStations.ManAirport);

        // All
        Response result = APIClient.getApiResponse(appExtension, "stations/recent", cookie);
        assertEquals(200, result.getStatus());

        List<StationRefDTO> stationDtos = result.readEntity(new GenericType<>() {});

        assertEquals(3, stationDtos.size());

        Set<String> ids = stationDtos.stream().map(StationRefDTO::getId).collect(Collectors.toSet());

        assertTrue(ids.contains(TramStations.Altrincham.forDTO()));
        assertTrue(ids.contains(TramStations.Bury.forDTO()));
        assertTrue(ids.contains(TramStations.ManAirport.forDTO()));

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
