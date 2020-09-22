package com.tramchester.integration.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.App;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.StationClosure;
import com.tramchester.domain.Timestamped;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.*;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.RecentJourneys;
import com.tramchester.integration.IntegrationAppExtension;
import com.tramchester.integration.IntegrationClient;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramStations;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(DropwizardExtensionsSupport.class)
class StationResourceTest {

    private static final IntegrationAppExtension appExtension = new IntegrationAppExtension(App.class, new ClosedStationTestConfig());

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldGetSingleStationWithPlatforms() {
        String id = TramStations.StPetersSquare.forDTO();
        String endPoint = "stations/" + id;
        Response response = IntegrationClient.getApiResponse(appExtension, endPoint);
        Assertions.assertEquals(200,response.getStatus());
        LocationDTO result = response.readEntity(LocationDTO.class);

        Assertions.assertEquals(id, result.getId());

        List<PlatformDTO> platforms = result.getPlatforms();
        Assertions.assertEquals(4, platforms.size());
        Assertions.assertEquals(id+"1", platforms.get(0).getId());
        Assertions.assertEquals(id+"2", platforms.get(1).getId());
        Assertions.assertEquals(id+"3", platforms.get(2).getId());
        Assertions.assertEquals(id+"4", platforms.get(3).getId());

        List<RouteRefDTO> routes = result.getRoutes();
        assertEquals(8, routes.size()); // 2 x 4
    }

    @Test
    void shouldGetAllStations() {
        Response result = IntegrationClient.getApiResponse(appExtension, "stations/all");

        assertEquals(200, result.getStatus());

        List<StationRefDTO> results = result.readEntity(new GenericType<>() {});

        App app =  appExtension.getApplication();
        StationRepository stationRepo = app.getDependencies().get(StationRepository.class);
        Set<String> stationsIds = stationRepo.getStations().stream().map(station -> station.getId().forDTO()).collect(Collectors.toSet());

        assertEquals(stationsIds.size(), results.size());

        Set<String> resultIds = results.stream().map(StationRefDTO::getId).collect(Collectors.toSet());

        assertTrue(stationsIds.containsAll(resultIds));
    }

    @Test
    void shouldGetTramStations() {
        Response result = IntegrationClient.getApiResponse(appExtension, "stations/mode/Tram");

        assertEquals(200, result.getStatus());

        List<StationRefDTO> results = result.readEntity(new GenericType<>() {});

        App app =  appExtension.getApplication();
        StationRepository stationRepo = app.getDependencies().get(StationRepository.class);
        Set<String> stationsIds = stationRepo.getStations().stream().map(station -> station.getId().forDTO()).collect(Collectors.toSet());

        assertEquals(stationsIds.size(), results.size());

        Set<String> resultIds = results.stream().map(StationRefDTO::getId).collect(Collectors.toSet());

        assertTrue(stationsIds.containsAll(resultIds));
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

        assertEquals(6,stationList.size());
        Set<String> ids = stationList.stream().map(StationRefDTO::getId).collect(Collectors.toSet());
        assertTrue(ids.contains(TramStations.PiccadillyGardens.forDTO()));
        assertTrue(ids.contains(TramStations.Piccadilly.forDTO()));
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
