package com.tramchester.integration.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.App;
import com.tramchester.domain.Timestamped;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.ProximityGroups;
import com.tramchester.domain.presentation.DTO.DTO;
import com.tramchester.domain.presentation.DTO.LocationDTO;
import com.tramchester.domain.presentation.DTO.StationDTO;
import com.tramchester.domain.presentation.DTO.StationListDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.ProximityGroup;
import com.tramchester.domain.presentation.RecentJourneys;
import com.tramchester.integration.IntegrationClient;
import com.tramchester.integration.IntegrationAppExtension;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.testSupport.Stations;
import com.tramchester.testSupport.TestEnv;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(DropwizardExtensionsSupport.class)
class StationResourceTest {

    private static final IntegrationAppExtension appExtension = new IntegrationAppExtension(App.class, new IntegrationTramTestConfig());

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldGetSingleStationWithPlatforms() {
        String id = Stations.StPetersSquare.getId();
        String endPoint = "stations/" + id;
        Response response = IntegrationClient.getApiResponse(appExtension, endPoint, Optional.empty(), 200);
        Assertions.assertEquals(200,response.getStatus());
        StationDTO result = response.readEntity(StationDTO.class);

        Assertions.assertEquals(id, result.getId());

        List<DTO> platforms = result.getPlatforms();
        Assertions.assertEquals(4, platforms.size());
        Assertions.assertEquals(id+"1", platforms.get(0).getId());
        Assertions.assertEquals(id+"2", platforms.get(1).getId());
        Assertions.assertEquals(id+"3", platforms.get(2).getId());
        Assertions.assertEquals(id+"4", platforms.get(3).getId());
    }

    @Test
    void shouldGetNearestStations() {
        StationListDTO stationListDTO = getNearest(TestEnv.nearPiccGardens, Optional.empty());
        List<StationDTO> stations = stationListDTO.getStations();

        Map<ProximityGroup, Long> stationGroups = stations.stream()
                .collect(Collectors.groupingBy(StationDTO::getProximityGroup, Collectors.counting()));

        Long one = 1L;
        Assertions.assertEquals(one, stationGroups.get(ProximityGroups.MY_LOCATION));
        Assertions.assertTrue(stationGroups.get(ProximityGroups.NEAREST_STOPS) > 0);
        int ALL_STOPS_START = 7; // 6 + 1
        Assertions.assertEquals(ProximityGroups.STOPS, stations.get(ALL_STOPS_START).getProximityGroup());
        Assertions.assertEquals("Abraham Moss", stations.get(ALL_STOPS_START).getName());
        StationDTO stationDTO = stations.get(ALL_STOPS_START + 1);
        Assertions.assertEquals("Altrincham", stationDTO.getName());

        List<DTO> platforms = stationDTO.getPlatforms();
        Assertions.assertEquals(1, platforms.size());
        DTO platformDTO = platforms.get(0);
        Assertions.assertEquals("1", platformDTO.getPlatformNumber());
        Assertions.assertEquals("Altrincham platform 1", platformDTO.getName());
        Assertions.assertEquals(Stations.Altrincham.getId()+"1", platformDTO.getId());

        List<ProximityGroup> proximityGroups = stationListDTO.getProximityGroups();
        checkProximityGroupsForTrams(proximityGroups);
    }

    private void checkProximityGroupsForTrams(List<ProximityGroup> proximityGroups) {
        Assertions.assertEquals(4, proximityGroups.size());
        Set<String> names = proximityGroups.stream().map(ProximityGroup::getName).collect(Collectors.toSet());
        Assertions.assertTrue(names.contains(ProximityGroups.STOPS.getName()));
        Assertions.assertTrue(names.contains(ProximityGroups.NEAREST_STOPS.getName()));
        Assertions.assertTrue(names.contains(ProximityGroups.RECENT.getName()));
        Assertions.assertTrue(names.contains(ProximityGroups.MY_LOCATION.getName()));
    }

    @Test
    void shouldGetMyLocationPlaceholderStation() {
        List<StationDTO> stations = getNearest(TestEnv.nearPiccGardens, Optional.empty()).getStations();

        StationDTO station = stations.get(0);
        Assertions.assertEquals(ProximityGroups.MY_LOCATION, station.getProximityGroup());
        Assertions.assertEquals("MyLocationPlaceholderId", station.getId());
        String expectedArea = String.format("{\"lat\":%s,\"lon\":%s}",
                TestEnv.nearPiccGardens.getLat(), TestEnv.nearPiccGardens.getLon());
        Assertions.assertEquals(expectedArea, station.getArea());
        Assertions.assertEquals("My Location", station.getName());
        // the nearest stops show come next
        Assertions.assertEquals(ProximityGroups.NEAREST_STOPS, stations.get(1).getProximityGroup());

    }

    @Test
    void shouldNotGetMyLocationPlaceholderStationWhenGettingAllStations() {
        getNearest(TestEnv.nearPiccGardens, Optional.empty());
        Collection<StationDTO> stations = getAll(Optional.empty()).getStations();

        stations.forEach(station -> Assertions.assertNotEquals("My Location", station.getName()));
    }

    @Test
    void shouldNotGetClosedStations() {
        Collection<StationDTO> stations = getAll(Optional.empty()).getStations();

        assertThat(stations.stream().filter(station -> station.getName().equals("St Peters Square")).count()).isEqualTo(0);
        assertThat(stations.stream().filter(station -> station.getName().equals(Stations.Altrincham.getName())).count()).isEqualTo(1);
    }

    @Test
    void shouldGetAllStationsWithRightOrderAndProxGroup() {
        StationListDTO stationListDTO = getAll(Optional.empty());
        Collection<StationDTO> stations = stationListDTO.getStations();

        assertThat(stations.stream().findFirst().get().getName()).isEqualTo("Abraham Moss");
        stations.forEach(station -> assertThat(station.getProximityGroup()).isEqualTo(ProximityGroups.STOPS));

        List<ProximityGroup> proximityGroups = stationListDTO.getProximityGroups();
        checkProximityGroupsForTrams(proximityGroups);
    }

    @Test
    void shouldReturnRecentAllStationsGroupIfCookieSet() throws JsonProcessingException {
        Location alty = Stations.Altrincham;
        Cookie cookie = createRecentsCookieFor(alty);

        // All
        List<StationDTO> stations = getAll(Optional.of(cookie)).getStations();
        stations.removeIf(station -> !station.getId().equals(alty.getId()));
        Assertions.assertEquals(1, stations.size());
        Assertions.assertEquals(ProximityGroups.RECENT, stations.get(0).getProximityGroup());
    }

    @Test
    void shouldReturnRecentAllAndNearbyStationsGroupIfCookieSet() throws JsonProcessingException {
        Location alty = Stations.Altrincham;
        @NotNull Cookie cookie = createRecentsCookieFor(alty);

        // All with nearest
        List<StationDTO> stations = getNearest(TestEnv.nearPiccGardens, Optional.of(cookie)).getStations();
        stations.removeIf(station -> !station.getId().equals(alty.getId()));
        Assertions.assertEquals(1, stations.size());
        Assertions.assertEquals(ProximityGroups.RECENT, stations.get(0).getProximityGroup());
    }

    @Test
    void shouldReturnChangesToRecent() throws JsonProcessingException {
        Cookie cookieA = createRecentsCookieFor(Stations.Altrincham);

        Response result = IntegrationClient.getApiResponse(appExtension, "stations/update", Optional.of(cookieA), 200);
        StationListDTO list = result.readEntity(StationListDTO.class);

        Assertions.assertEquals(1, list.getProximityGroups().size());
        Assertions.assertEquals(1, list.getStations().size());
        Assertions.assertEquals(Stations.Altrincham.getId(), list.getStations().get(0).getId());

        Cookie cookieB = createRecentsCookieFor(Stations.Altrincham, Stations.Deansgate);

        Response updatedResult = IntegrationClient.getApiResponse(appExtension, "stations/update", Optional.of(cookieB), 200);
        StationListDTO updatedList = updatedResult.readEntity(StationListDTO.class);

        Assertions.assertEquals(1, updatedList.getProximityGroups().size());
        Assertions.assertEquals(2, updatedList.getStations().size());
        Assertions.assertEquals(Stations.Deansgate.getId(), updatedList.getStations().get(0).getId());
        Assertions.assertEquals(Stations.Altrincham.getId(), updatedList.getStations().get(1).getId());
    }

    @Test
    void shouldReturnChangesToRecentAndNearest() throws JsonProcessingException {

        Response result = IntegrationClient.getApiResponse(appExtension, String.format("stations/update/%s/%s",
                TestEnv.nearPiccGardens.getLat(), TestEnv.nearPiccGardens.getLon()),
                Optional.of(createRecentsCookieFor(Stations.Bury)), 200);

        StationListDTO list = result.readEntity(StationListDTO.class);
        Assertions.assertEquals(2, list.getProximityGroups().size());

        List<StationDTO> recents = list.getStations().stream().
                filter(stationDTO -> stationDTO.getProximityGroup().equals(ProximityGroups.RECENT)).collect(Collectors.toList());
        Assertions.assertEquals(1, recents.size());
        Assertions.assertEquals(Stations.Bury.getId(), recents.get(0).getId());

        List<StationDTO> nearby = list.getStations().stream().
                filter(stationDTO -> stationDTO.getProximityGroup().equals(ProximityGroups.NEAREST_STOPS)).collect(Collectors.toList());
        Assertions.assertEquals(6, nearby.size());

        // add one of the nearby to the recents list
        result = IntegrationClient.getApiResponse(appExtension, String.format("stations/update/%s/%s",
                TestEnv.nearPiccGardens.getLat(), TestEnv.nearPiccGardens.getLon()),
                Optional.of(createRecentsCookieFor(Stations.PiccadillyGardens)), 200);
        list = result.readEntity(StationListDTO.class);

        recents = list.getStations().stream().
                filter(stationDTO -> stationDTO.getProximityGroup().equals(ProximityGroups.RECENT)).collect(Collectors.toList());
        Assertions.assertEquals(1, recents.size());
        Assertions.assertEquals(Stations.PiccadillyGardens.getId(), recents.get(0).getId());
        nearby = list.getStations().stream().
                filter(stationDTO -> stationDTO.getProximityGroup().equals(ProximityGroups.NEAREST_STOPS)).collect(Collectors.toList());
        Assertions.assertEquals(5, nearby.size());

        // switch locations, expect different nearby stations
        result = IntegrationClient.getApiResponse(appExtension, String.format("stations/update/%s/%s",
                TestEnv.nearAltrincham.getLat(), TestEnv.nearAltrincham.getLon()),
                Optional.of(createRecentsCookieFor(Stations.PiccadillyGardens)), 200);
        list = result.readEntity(StationListDTO.class);
        List<StationDTO> updatedNearby = list.getStations().stream().
                filter(stationDTO -> stationDTO.getProximityGroup().equals(ProximityGroups.NEAREST_STOPS)).collect(Collectors.toList());
        Assertions.assertEquals(2, updatedNearby.size());

        Set<String> firstNearbyIds = nearby.stream().map(LocationDTO::getId).collect(Collectors.toSet());
        Set<String> updatedNearbyIds = updatedNearby.stream().map(LocationDTO::getId).collect(Collectors.toSet());

        firstNearbyIds.retainAll(updatedNearbyIds);
        Assertions.assertTrue(firstNearbyIds.isEmpty());

    }

    @NotNull
    private Cookie createRecentsCookieFor(Location... locations) throws JsonProcessingException {
        RecentJourneys recentJourneys = new RecentJourneys();

        Set<Timestamped> recents = new HashSet<>();
        for (Location location : locations) {
            Timestamped timestamped = new Timestamped(location.getId(), TestEnv.LocalNow());
            recents.add(timestamped);
        }
        recentJourneys.setTimestamps(recents);

        String recentAsString = RecentJourneys.encodeCookie(mapper,recentJourneys);
        return new Cookie("tramchesterRecent", recentAsString);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private StationListDTO getNearest(LatLong location, Optional<Cookie> cookie) {
        Response result = IntegrationClient.getApiResponse(appExtension, String.format("stations/%s/%s",
                location.getLat(), location.getLon()), cookie, 200);
        return result.readEntity(StationListDTO.class);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private StationListDTO getAll(Optional<Cookie> cookie) {
        Response result = IntegrationClient.getApiResponse(appExtension, "stations", cookie, 200);
        return result.readEntity(StationListDTO.class);
    }

}
