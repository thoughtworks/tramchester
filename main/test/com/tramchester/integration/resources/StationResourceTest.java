package com.tramchester.integration.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.App;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.StationClosure;
import com.tramchester.domain.Timestamped;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.*;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.RecentJourneys;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.IntegrationClient;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(DropwizardExtensionsSupport.class)
class StationResourceTest {

    private static final IntegrationAppExtension appExtension = new IntegrationAppExtension(App.class, new ClosedStationTestConfig());

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldGetSingleStationWithPlatforms() {
        String stationId = TramStations.StPetersSquare.forDTO();
        String endPoint = "stations/" + stationId;
        Response response = IntegrationClient.getApiResponse(appExtension, endPoint);
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

        List<RouteRefDTO> routes = result.getRoutes();
        assertEquals(8, routes.size()); // 2 x 4
    }

    @Test
    void shouldGetTramStations() {
        Response result = IntegrationClient.getApiResponse(appExtension, "stations/mode/Tram");

        assertEquals(200, result.getStatus());

        List<StationRefDTO> results = result.readEntity(new GenericType<>() {});

        App app =  appExtension.getApplication();
        StationRepository stationRepo = app.getDependencies().get(StationRepository.class);

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
        Response resultA = IntegrationClient.getApiResponse(appExtension, "stations/mode/Tram");
        assertEquals(200, resultA.getStatus());

        Date lastMod = resultA.getLastModified();

        Response resultB = IntegrationClient.getApiResponse(appExtension, "stations/mode/Tram", lastMod);
        assertEquals(304, resultB.getStatus());
    }

    @Test
    void shouldGetBusStations() {
        Response result = IntegrationClient.getApiResponse(appExtension, "stations/mode/Bus");

        assertEquals(200, result.getStatus());

        // buses disabled, but should still get a list back, albeit empty
        List<StationRefDTO> results = result.readEntity(new GenericType<>() {});
        assertEquals(0, results.size());
    }

    @Test
    void should404ForUnknownMode() {
        Response result = IntegrationClient.getApiResponse(appExtension, "stations/mode/Jumping");
        assertEquals(404, result.getStatus());
    }

    @Test
    void shouldGetNearestStations() {

        LatLong nearPiccGardens = TestEnv.nearPiccGardens;
        Response result = IntegrationClient.getApiResponse(appExtension, String.format("stations/near?lat=%s&lon=%s",
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
        Response result = IntegrationClient.getApiResponse(appExtension, "stations/recent", cookie);
        assertEquals(200, result.getStatus());

        List<StationRefDTO> stationDtos = result.readEntity(new GenericType<>() {});

        assertEquals(3, stationDtos.size());

        Set<String> ids = stationDtos.stream().map(StationRefDTO::getId).collect(Collectors.toSet());

        assertTrue(ids.contains(TramStations.Altrincham.forDTO()));
        assertTrue(ids.contains(TramStations.Bury.forDTO()));
        assertTrue(ids.contains(TramStations.ManAirport.forDTO()));

    }

    @Test
    void shouldGetClosedStations() {
        Response result = IntegrationClient.getApiResponse(appExtension, "stations/closures");
        assertEquals(200, result.getStatus());

        List<StationClosureDTO> results = result.readEntity(new GenericType<>() {});

        assertEquals(1, results.size());
        StationClosureDTO stationClosure = results.get(0);
        assertEquals(TramStations.StPetersSquare.forDTO(), stationClosure.getStation().getId());
        assertEquals(TestEnv.testDay(), stationClosure.getBegin());
        assertEquals(TestEnv.testDay().plusWeeks(1), stationClosure.getEnd());
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

    private static class ClosedStationTestConfig extends IntegrationTramTestConfig {

        @Override
        public List<StationClosure> getStationClosures() {
            return closedStations;
        }

        private final List<StationClosure> closedStations = Collections.singletonList(
                new StationClosure() {
                    @Override
                    public IdFor<Station> getStation() {
                        return TramStations.StPetersSquare.getId();
                    }

                    @Override
                    public LocalDate getBegin() {
                        return TestEnv.testDay();
                    }

                    @Override
                    public LocalDate getEnd() {
                        return getBegin().plusWeeks(1);
                    }
                });
    }
}
