package com.tramchester.integration.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.App;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.StationClosure;
import com.tramchester.domain.Timestamped;
import com.tramchester.domain.places.ProximityGroups;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.*;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.ProximityGroup;
import com.tramchester.domain.presentation.RecentJourneys;
import com.tramchester.integration.IntegrationAppExtension;
import com.tramchester.integration.IntegrationClient;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.Stations;
import com.tramchester.testSupport.TestEnv;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(DropwizardExtensionsSupport.class)
class StationResourceTest {

    private static final IntegrationAppExtension appExtension = new IntegrationAppExtension(App.class, new ClosedStationTestConfig());

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldGetSingleStationWithPlatforms() {
        String id = Stations.StPetersSquare.getId().forDTO();
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

    @Deprecated
    @Test
    void shouldGetNearestStationsOLDAPI() {
        StationListDTO stationListDTO = getNearest(TestEnv.nearPiccGardens);
        List<StationRefWithGroupDTO> stations = stationListDTO.getStations();

        Map<ProximityGroup, Long> stationGroups = stations.stream()
                .collect(Collectors.groupingBy(StationRefWithGroupDTO::getProximityGroup, Collectors.counting()));

        Long one = 1L;
        Assertions.assertEquals(one, stationGroups.get(ProximityGroups.MY_LOCATION));
        assertTrue(stationGroups.get(ProximityGroups.NEAREST_STOPS) > 0);
        int ALL_STOPS_START = 7; // 6 + 1
        Assertions.assertEquals(ProximityGroups.STOPS, stations.get(ALL_STOPS_START).getProximityGroup());
        Assertions.assertEquals("Abraham Moss", stations.get(ALL_STOPS_START).getName());
        StationRefWithGroupDTO stationDTO = stations.get(ALL_STOPS_START + 1);
        Assertions.assertEquals("Altrincham", stationDTO.getName());

        List<ProximityGroup> proximityGroups = stationListDTO.getProximityGroups();
        checkProximityGroupsForTrams(proximityGroups);
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
    void shouldGetNearestStations() {

        LatLong nearPiccGardens = TestEnv.nearPiccGardens;
        Response result = IntegrationClient.getApiResponse(appExtension, String.format("stations/near?lat=%s&lon=%s",
                nearPiccGardens.getLat(), nearPiccGardens.getLon()));
        assertEquals(200, result.getStatus());

        List<StationRefDTO> stationList = result.readEntity(new GenericType<>() {});

        assertEquals(6,stationList.size());
        Set<String> ids = stationList.stream().map(StationRefDTO::getId).collect(Collectors.toSet());
        assertTrue(ids.contains(Stations.PiccadillyGardens.forDTO()));
        assertTrue(ids.contains(Stations.Piccadilly.forDTO()));
        assertTrue(ids.contains(Stations.StPetersSquare.forDTO()));
        assertTrue(ids.contains(Stations.MarketStreet.forDTO()));
        assertTrue(ids.contains(Stations.ExchangeSquare.forDTO()));
        assertTrue(ids.contains(Stations.Shudehill.forDTO()));
    }

    @Test
    void shouldGetRecentStations() throws JsonProcessingException {
        Cookie cookie = createRecentsCookieFor(Stations.Altrincham, Stations.Bury, Stations.ManAirport);

        // All
        Response result = IntegrationClient.getApiResponse(appExtension, "stations/recent", cookie);
        assertEquals(200, result.getStatus());

        List<StationRefDTO> stationDtos = result.readEntity(new GenericType<>() {});

        assertEquals(3, stationDtos.size());

        Set<String> ids = stationDtos.stream().map(StationRefDTO::getId).collect(Collectors.toSet());

        assertTrue(ids.contains(Stations.Altrincham.forDTO()));
        assertTrue(ids.contains(Stations.Bury.forDTO()));
        assertTrue(ids.contains(Stations.ManAirport.forDTO()));

    }

    private void checkProximityGroupsForTrams(List<ProximityGroup> proximityGroups) {
        Assertions.assertEquals(4, proximityGroups.size());
        Set<String> names = proximityGroups.stream().map(ProximityGroup::getName).collect(Collectors.toSet());
        assertTrue(names.contains(ProximityGroups.STOPS.getName()));
        assertTrue(names.contains(ProximityGroups.NEAREST_STOPS.getName()));
        assertTrue(names.contains(ProximityGroups.RECENT.getName()));
        assertTrue(names.contains(ProximityGroups.MY_LOCATION.getName()));
    }

    @Test
    void shouldGetMyLocationPlaceholderStationOLDAPI() {
        List<StationRefWithGroupDTO> stations = getNearest(TestEnv.nearPiccGardens).getStations();

        StationRefWithGroupDTO station = stations.get(0);
        Assertions.assertEquals(ProximityGroups.MY_LOCATION, station.getProximityGroup());
        Assertions.assertEquals("MyLocationPlaceholderId", station.getId());

        Assertions.assertEquals("My Location", station.getName());
        // the nearest stops show come next
        Assertions.assertEquals(ProximityGroups.NEAREST_STOPS, stations.get(1).getProximityGroup());

    }

    @Test
    void shouldGetClosedStations() {
        Response result = IntegrationClient.getApiResponse(appExtension, "stations/closures");
        assertEquals(200, result.getStatus());

        List<StationClosureDTO> results = result.readEntity(new GenericType<>() {});

        assertEquals(1, results.size());
        StationClosureDTO stationClosure = results.get(0);
        assertEquals(Stations.StPetersSquare.forDTO(), stationClosure.getStation().getId());
        assertEquals(TestEnv.testDay(), stationClosure.getBegin());
        assertEquals(TestEnv.testDay().plusWeeks(1), stationClosure.getEnd());
    }

    @Deprecated
    @Test
    void shouldNotGetMyLocationPlaceholderStationWhenGettingAllStations() {
        getNearest(TestEnv.nearPiccGardens);
        Collection<StationRefWithGroupDTO> stations = getAll().getStations();

        stations.forEach(station -> Assertions.assertNotEquals("My Location", station.getName()));
    }

    @Deprecated
    @Test
    void shouldGetAllStationsWithRightOrderAndProxGroup() {
        StationListDTO stationListDTO = getAll();
        Collection<StationRefWithGroupDTO> stations = stationListDTO.getStations();

        assertThat(stations.stream().findFirst().get().getName()).isEqualTo("Abraham Moss");
        stations.forEach(station -> assertThat(station.getProximityGroup()).isEqualTo(ProximityGroups.STOPS));

        List<ProximityGroup> proximityGroups = stationListDTO.getProximityGroups();
        checkProximityGroupsForTrams(proximityGroups);
    }


    @Deprecated
    @Test
    void shouldReturnRecentAllStationsGroupIfCookieSet() throws JsonProcessingException {
        Station alty = Stations.Altrincham;
        Cookie cookie = createRecentsCookieFor(alty);

        // All
        List<StationRefWithGroupDTO> stationDtos = getAll(cookie).getStations();
        stationDtos.removeIf(station -> !station.getId().equals(alty.getId().forDTO()));
        Assertions.assertEquals(1, stationDtos.size());
        Assertions.assertEquals(ProximityGroups.RECENT, stationDtos.get(0).getProximityGroup());
    }

    @Deprecated
    @Test
    void shouldReturnRecentAllAndNearbyStationsGroupIfCookieSet() throws JsonProcessingException {
        Station alty = Stations.Altrincham;
        @NotNull Cookie cookie = createRecentsCookieFor(alty);

        // All with nearest
        List<StationRefWithGroupDTO> stationDtos = getNearest(TestEnv.nearPiccGardens, cookie).getStations();
        stationDtos.removeIf(station -> !station.getId().equals(alty.getId().forDTO()));
        Assertions.assertEquals(1, stationDtos.size());
        Assertions.assertEquals(ProximityGroups.RECENT, stationDtos.get(0).getProximityGroup());
    }

    @Test
    void shouldReturnChangesToRecent() throws JsonProcessingException {
        Cookie cookieA = createRecentsCookieFor(Stations.Altrincham);

        Response result = IntegrationClient.getApiResponse(appExtension, "stations/update", cookieA);
        assertEquals(200, result.getStatus());

        StationListDTO list = result.readEntity(StationListDTO.class);

        Assertions.assertEquals(1, list.getProximityGroups().size());
        Assertions.assertEquals(1, list.getStations().size());
        Assertions.assertEquals(Stations.Altrincham.getId().forDTO(), list.getStations().get(0).getId());

        Cookie cookieB = createRecentsCookieFor(Stations.Altrincham, Stations.Deansgate);

        Response updatedResult = IntegrationClient.getApiResponse(appExtension, "stations/update",cookieB);
        assertEquals(200, updatedResult.getStatus());

        StationListDTO updatedList = updatedResult.readEntity(StationListDTO.class);

        Assertions.assertEquals(1, updatedList.getProximityGroups().size());
        Assertions.assertEquals(2, updatedList.getStations().size());
        Assertions.assertEquals(Stations.Deansgate.getId().forDTO(), updatedList.getStations().get(0).getId());
        Assertions.assertEquals(Stations.Altrincham.getId().forDTO(), updatedList.getStations().get(1).getId());
    }

    @Test
    void shouldReturnChangesToRecentAndNearest() throws JsonProcessingException {

        Response result = IntegrationClient.getApiResponse(appExtension, String.format("stations/update/%s/%s",
                TestEnv.nearPiccGardens.getLat(), TestEnv.nearPiccGardens.getLon()),
                createRecentsCookieFor(Stations.Bury));
        assertEquals(200, result.getStatus());

        StationListDTO list = result.readEntity(StationListDTO.class);
        Assertions.assertEquals(2, list.getProximityGroups().size());

        List<StationRefWithGroupDTO> recents = list.getStations().stream().
                filter(stationDTO -> stationDTO.getProximityGroup().equals(ProximityGroups.RECENT)).collect(Collectors.toList());
        Assertions.assertEquals(1, recents.size());
        Assertions.assertEquals(Stations.Bury.getId().forDTO(), recents.get(0).getId());

        List<StationRefWithGroupDTO> nearby = list.getStations().stream().
                filter(stationDTO -> stationDTO.getProximityGroup().equals(ProximityGroups.NEAREST_STOPS)).collect(Collectors.toList());
        Assertions.assertEquals(6, nearby.size());

        // add one of the nearby to the recents list
        result = IntegrationClient.getApiResponse(appExtension, String.format("stations/update/%s/%s",
                TestEnv.nearPiccGardens.getLat(), TestEnv.nearPiccGardens.getLon()),
                createRecentsCookieFor(Stations.PiccadillyGardens));
        assertEquals(200, result.getStatus());

        list = result.readEntity(StationListDTO.class);

        recents = list.getStations().stream().
                filter(stationDTO -> stationDTO.getProximityGroup().equals(ProximityGroups.RECENT)).collect(Collectors.toList());
        Assertions.assertEquals(1, recents.size());
        Assertions.assertEquals(Stations.PiccadillyGardens.getId().forDTO(), recents.get(0).getId());
        nearby = list.getStations().stream().
                filter(stationDTO -> stationDTO.getProximityGroup().equals(ProximityGroups.NEAREST_STOPS)).collect(Collectors.toList());
        Assertions.assertEquals(5, nearby.size());

        // switch locations, expect different nearby stations
        result = IntegrationClient.getApiResponse(appExtension, String.format("stations/update/%s/%s",
                TestEnv.nearAltrincham.getLat(), TestEnv.nearAltrincham.getLon()),
                createRecentsCookieFor(Stations.PiccadillyGardens));
        assertEquals(200, result.getStatus());

        list = result.readEntity(StationListDTO.class);
        List<StationRefWithGroupDTO> updatedNearby = list.getStations().stream().
                filter(stationDTO -> stationDTO.getProximityGroup().equals(ProximityGroups.NEAREST_STOPS)).collect(Collectors.toList());
        Assertions.assertEquals(2, updatedNearby.size());

        Set<String> firstNearbyIds = nearby.stream().map(StationRefWithGroupDTO::getId).collect(Collectors.toSet());
        Set<String> updatedNearbyIds = updatedNearby.stream().map(StationRefWithGroupDTO::getId).collect(Collectors.toSet());

        firstNearbyIds.retainAll(updatedNearbyIds);
        assertTrue(firstNearbyIds.isEmpty());

    }

    @NotNull
    private Cookie createRecentsCookieFor(Station... stations) throws JsonProcessingException {
        RecentJourneys recentJourneys = new RecentJourneys();

        Set<Timestamped> recents = new HashSet<>();
        for (Station station : stations) {
            Timestamped timestamped = new Timestamped(station.getId(), TestEnv.LocalNow());
            recents.add(timestamped);
        }
        recentJourneys.setTimestamps(recents);

        String recentAsString = RecentJourneys.encodeCookie(mapper,recentJourneys);
        return new Cookie("tramchesterRecent", recentAsString);
    }

    private StationListDTO getNearest(LatLong location, Cookie cookie) {
        Response result = IntegrationClient.getApiResponse(appExtension, getLocationURL(location), cookie);
        assertEquals(200, result.getStatus());

        return result.readEntity(StationListDTO.class);
    }

    private String getLocationURL(LatLong location) {
        return String.format("stations/%s/%s",
                location.getLat(), location.getLon());
    }

    private StationListDTO getAll(Cookie cookie) {
        Response result = IntegrationClient.getApiResponse(appExtension, "stations", cookie);
        assertEquals(200, result.getStatus());

        return result.readEntity(StationListDTO.class);
    }

    private StationListDTO getNearest(LatLong location) {
        Response result = IntegrationClient.getApiResponse(appExtension, getLocationURL(location));
        assertEquals(200, result.getStatus());

        return result.readEntity(StationListDTO.class);
    }

    private StationListDTO getAll() {
        Response result = IntegrationClient.getApiResponse(appExtension, "stations");
        assertEquals(200, result.getStatus());

        return result.readEntity(StationListDTO.class);
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
                        return Stations.StPetersSquare.getId();
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
