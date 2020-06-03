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
import com.tramchester.integration.IntegrationTestRun;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.testSupport.Stations;
import com.tramchester.testSupport.TestEnv;
import org.jetbrains.annotations.NotNull;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

import static junit.framework.TestCase.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class StationResourceTest {

    @ClassRule
    public static IntegrationTestRun testRule = new IntegrationTestRun(App.class, new IntegrationTramTestConfig());

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void shouldGetSingleStationWithPlatforms() {
        String id = Stations.StPetersSquare.getId();
        String endPoint = "stations/" + id;
        Response response = IntegrationClient.getResponse(testRule, endPoint, Optional.empty(), 200);
        assertEquals(200,response.getStatus());
        StationDTO result = response.readEntity(StationDTO.class);

        assertEquals(id, result.getId());

        List<DTO> platforms = result.getPlatforms();
        assertEquals(4, platforms.size());
        assertEquals(id+"1", platforms.get(0).getId());
        assertEquals(id+"2", platforms.get(1).getId());
        assertEquals(id+"3", platforms.get(2).getId());
        assertEquals(id+"4", platforms.get(3).getId());
    }

    @Test
    public void shouldGetNearestStations() {
        StationListDTO stationListDTO = getNearest(TestEnv.nearPiccGardens, Optional.empty());
        List<StationDTO> stations = stationListDTO.getStations();

        Map<ProximityGroup, Long> stationGroups = stations.stream()
                .collect(Collectors.groupingBy(StationDTO::getProximityGroup, Collectors.counting()));

        Long one = 1L;
        assertEquals(one, stationGroups.get(ProximityGroups.MY_LOCATION));
        assertTrue(stationGroups.get(ProximityGroups.NEAREST_STOPS) > 0);
        int ALL_STOPS_START = 7; // 6 + 1
        assertEquals(ProximityGroups.STOPS, stations.get(ALL_STOPS_START).getProximityGroup());
        assertEquals("Abraham Moss", stations.get(ALL_STOPS_START).getName());
        StationDTO stationDTO = stations.get(ALL_STOPS_START + 1);
        assertEquals("Altrincham", stationDTO.getName());

        List<DTO> platforms = stationDTO.getPlatforms();
        assertEquals(1, platforms.size());
        DTO platformDTO = platforms.get(0);
        assertEquals("1", platformDTO.getPlatformNumber());
        assertEquals("Altrincham platform 1", platformDTO.getName());
        assertEquals(Stations.Altrincham.getId()+"1", platformDTO.getId());

        List<ProximityGroup> proximityGroups = stationListDTO.getProximityGroups();
        checkProximityGroupsForTrams(proximityGroups);
    }

    private void checkProximityGroupsForTrams(List<ProximityGroup> proximityGroups) {
        assertEquals(4, proximityGroups.size());
        Set<String> names = proximityGroups.stream().map(ProximityGroup::getName).collect(Collectors.toSet());
        assertTrue(names.contains(ProximityGroups.STOPS.getName()));
        assertTrue(names.contains(ProximityGroups.NEAREST_STOPS.getName()));
        assertTrue(names.contains(ProximityGroups.RECENT.getName()));
        assertTrue(names.contains(ProximityGroups.MY_LOCATION.getName()));
    }

    @Test
    public void shouldGetMyLocationPlaceholderStation() {
        List<StationDTO> stations = getNearest(TestEnv.nearPiccGardens, Optional.empty()).getStations();

        StationDTO station = stations.get(0);
        assertEquals(ProximityGroups.MY_LOCATION, station.getProximityGroup());
        assertEquals("MyLocationPlaceholderId", station.getId());
        String expectedArea = String.format("{\"lat\":%s,\"lon\":%s}",
                TestEnv.nearPiccGardens.getLat(), TestEnv.nearPiccGardens.getLon());
        assertEquals(expectedArea, station.getArea());
        assertEquals("My Location", station.getName());
        // the nearest stops show come next
        assertEquals(ProximityGroups.NEAREST_STOPS, stations.get(1).getProximityGroup());

    }

    @Test
    public void shouldNotGetMyLocationPlaceholderStationWhenGettingAllStations() {
        getNearest(TestEnv.nearPiccGardens, Optional.empty());
        Collection<StationDTO> stations = getAll(Optional.empty()).getStations();

        stations.forEach(station -> assertNotEquals("My Location", station.getName()));
    }

    @Test
    public void shouldNotGetClosedStations() {
        Collection<StationDTO> stations = getAll(Optional.empty()).getStations();

        assertThat(stations.stream().filter(station -> station.getName().equals("St Peters Square")).count()).isEqualTo(0);
        assertThat(stations.stream().filter(station -> station.getName().equals(Stations.Altrincham.getName())).count()).isEqualTo(1);
    }

    @Test
    public void shouldGetAllStationsWithRightOrderAndProxGroup() {
        StationListDTO stationListDTO = getAll(Optional.empty());
        Collection<StationDTO> stations = stationListDTO.getStations();

        assertThat(stations.stream().findFirst().get().getName()).isEqualTo("Abraham Moss");
        stations.forEach(station -> assertThat(station.getProximityGroup()).isEqualTo(ProximityGroups.STOPS));

        List<ProximityGroup> proximityGroups = stationListDTO.getProximityGroups();
        checkProximityGroupsForTrams(proximityGroups);
    }

    @Test
    public void shouldReturnRecentAllStationsGroupIfCookieSet() throws JsonProcessingException {
        Location alty = Stations.Altrincham;
        Cookie cookie = createRecentsCookieFor(alty);

        // All
        List<StationDTO> stations = getAll(Optional.of(cookie)).getStations();
        stations.removeIf(station -> !station.getId().equals(alty.getId()));
        assertEquals(1, stations.size());
        assertEquals(ProximityGroups.RECENT, stations.get(0).getProximityGroup());
    }

    @Test
    public void shouldReturnRecentAllAndNearbyStationsGroupIfCookieSet() throws JsonProcessingException {
        Location alty = Stations.Altrincham;
        @NotNull Cookie cookie = createRecentsCookieFor(alty);

        // All with nearest
        List<StationDTO> stations = getNearest(TestEnv.nearPiccGardens, Optional.of(cookie)).getStations();
        stations.removeIf(station -> !station.getId().equals(alty.getId()));
        assertEquals(1, stations.size());
        assertEquals(ProximityGroups.RECENT, stations.get(0).getProximityGroup());
    }

    @Test
    public void shouldReturnChangesToRecent() throws JsonProcessingException {
        Cookie cookieA = createRecentsCookieFor(Stations.Altrincham);

        Response result = IntegrationClient.getResponse(testRule, "stations/update", Optional.of(cookieA), 200);
        StationListDTO list = result.readEntity(StationListDTO.class);

        assertEquals(1, list.getProximityGroups().size());
        assertEquals(1, list.getStations().size());
        assertEquals(Stations.Altrincham.getId(), list.getStations().get(0).getId());

        Cookie cookieB = createRecentsCookieFor(Stations.Altrincham, Stations.Deansgate);

        Response updatedResult = IntegrationClient.getResponse(testRule, "stations/update", Optional.of(cookieB), 200);
        StationListDTO updatedList = updatedResult.readEntity(StationListDTO.class);

        assertEquals(1, updatedList.getProximityGroups().size());
        assertEquals(2, updatedList.getStations().size());
        assertEquals(Stations.Deansgate.getId(), updatedList.getStations().get(0).getId());
        assertEquals(Stations.Altrincham.getId(), updatedList.getStations().get(1).getId());
    }

    @Test
    public void shouldReturnChangesToRecentAndNearest() throws JsonProcessingException {

        Response result = IntegrationClient.getResponse(testRule, String.format("stations/update/%s/%s",
                TestEnv.nearPiccGardens.getLat(), TestEnv.nearPiccGardens.getLon()),
                Optional.of(createRecentsCookieFor(Stations.Bury)), 200);

        StationListDTO list = result.readEntity(StationListDTO.class);
        assertEquals(2, list.getProximityGroups().size());

        List<StationDTO> recents = list.getStations().stream().
                filter(stationDTO -> stationDTO.getProximityGroup().equals(ProximityGroups.RECENT)).collect(Collectors.toList());
        assertEquals(1, recents.size());
        assertEquals(Stations.Bury.getId(), recents.get(0).getId());

        List<StationDTO> nearby = list.getStations().stream().
                filter(stationDTO -> stationDTO.getProximityGroup().equals(ProximityGroups.NEAREST_STOPS)).collect(Collectors.toList());
        assertEquals(6, nearby.size());

        // add one of the nearby to the recents list
        result = IntegrationClient.getResponse(testRule, String.format("stations/update/%s/%s",
                TestEnv.nearPiccGardens.getLat(), TestEnv.nearPiccGardens.getLon()),
                Optional.of(createRecentsCookieFor(Stations.PiccadillyGardens)), 200);
        list = result.readEntity(StationListDTO.class);

        recents = list.getStations().stream().
                filter(stationDTO -> stationDTO.getProximityGroup().equals(ProximityGroups.RECENT)).collect(Collectors.toList());
        assertEquals(1, recents.size());
        assertEquals(Stations.PiccadillyGardens.getId(), recents.get(0).getId());
        nearby = list.getStations().stream().
                filter(stationDTO -> stationDTO.getProximityGroup().equals(ProximityGroups.NEAREST_STOPS)).collect(Collectors.toList());
        assertEquals(5, nearby.size());

        // switch locations, expect different nearby stations
        result = IntegrationClient.getResponse(testRule, String.format("stations/update/%s/%s",
                TestEnv.nearAltrincham.getLat(), TestEnv.nearAltrincham.getLon()),
                Optional.of(createRecentsCookieFor(Stations.PiccadillyGardens)), 200);
        list = result.readEntity(StationListDTO.class);
        List<StationDTO> updatedNearby = list.getStations().stream().
                filter(stationDTO -> stationDTO.getProximityGroup().equals(ProximityGroups.NEAREST_STOPS)).collect(Collectors.toList());
        assertEquals(2, updatedNearby.size());

        Set<String> firstNearbyIds = nearby.stream().map(LocationDTO::getId).collect(Collectors.toSet());
        Set<String> updatedNearbyIds = updatedNearby.stream().map(LocationDTO::getId).collect(Collectors.toSet());

        firstNearbyIds.retainAll(updatedNearbyIds);
        assertTrue(firstNearbyIds.isEmpty());

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
        Response result = IntegrationClient.getResponse(testRule, String.format("stations/%s/%s",
                location.getLat(), location.getLon()), cookie, 200);
        return result.readEntity(StationListDTO.class);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private StationListDTO getAll(Optional<Cookie> cookie) {
        Response result = IntegrationClient.getResponse(testRule, "stations", cookie, 200);
        return result.readEntity(StationListDTO.class);
    }

}
