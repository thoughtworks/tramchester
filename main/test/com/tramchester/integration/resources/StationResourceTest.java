package com.tramchester.integration.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import com.tramchester.App;
import com.tramchester.domain.Timestamped;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.presentation.DTO.DTO;
import com.tramchester.domain.presentation.DTO.StationDTO;
import com.tramchester.domain.presentation.DTO.StationListDTO;
import com.tramchester.domain.presentation.ProximityGroup;
import com.tramchester.domain.presentation.RecentJourneys;
import com.tramchester.integration.IntegrationClient;
import com.tramchester.integration.IntegrationTestRun;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.testSupport.Stations;
import com.tramchester.testSupport.TestEnv;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
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
        StationListDTO stationListDTO = getNearest(53.4804263d, -2.2392436d, Optional.empty());
        List<StationDTO> stations = stationListDTO.getStations();

        Map<ProximityGroup, Long> stationGroups = stations.stream()
                .collect(Collectors.groupingBy(StationDTO::getProximityGroup, Collectors.counting()));

        Long one = 1L;
        assertEquals(one, stationGroups.get(ProximityGroup.MY_LOCATION));
        assertTrue(stationGroups.get(ProximityGroup.NEAREST_STOPS) > 0);
        int ALL_STOPS_START = 7; // 6 + 1
        assertEquals(ProximityGroup.ALL, stations.get(ALL_STOPS_START).getProximityGroup());
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
        assertEquals(4, proximityGroups.size());
        Set<String> names = proximityGroups.stream().map(ProximityGroup::getName).collect(Collectors.toSet());
        assertTrue(names.contains(ProximityGroup.ALL.getName()));
        assertTrue(names.contains(ProximityGroup.NEAREST_STOPS.getName()));
        assertTrue(names.contains(ProximityGroup.RECENT.getName()));
        assertTrue(names.contains(ProximityGroup.MY_LOCATION.getName()));
    }

    @Test
    public void shouldGetSpecialStationWithMyLocation() {
        List<StationDTO> stations = getNearest(53.4804263d, -2.2392436d, Optional.empty()).getStations();

        StationDTO station = stations.get(0);
        assertEquals(ProximityGroup.MY_LOCATION, station.getProximityGroup());
        assertEquals("MyLocationPlaceholderId", station.getId());
        assertEquals("{\"lat\":53.4804263,\"lon\":-2.2392436}", station.getArea());
        assertEquals("My Location", station.getName());
        // the nearest stops show come next
        assertEquals(ProximityGroup.NEAREST_STOPS, stations.get(1).getProximityGroup());

    }

    @Test
    public void shouldNotGetSpecialStationWhenGettingAllStations() {
        getNearest(53.4804263d, -2.2392436d, Optional.empty());
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
        stations.forEach(station -> assertThat(station.getProximityGroup()).isEqualTo(ProximityGroup.ALL));

        List<ProximityGroup> proximityGroups = stationListDTO.getProximityGroups();
        assertEquals(4, proximityGroups.size());
        Set<String> names = proximityGroups.stream().map(ProximityGroup::getName).collect(Collectors.toSet());
        assertTrue(names.contains(ProximityGroup.ALL.getName()));
        assertTrue(names.contains(ProximityGroup.NEAREST_STOPS.getName()));
        assertTrue(names.contains(ProximityGroup.RECENT.getName()));
        assertTrue(names.contains(ProximityGroup.MY_LOCATION.getName()));
    }

    @Test
    public void shouldReturnRecentStationsGroupIfCookieSet() throws JsonProcessingException, UnsupportedEncodingException {
        Location alty = Stations.Altrincham;
        RecentJourneys recentJourneys = new RecentJourneys();
        recentJourneys.setTimestamps(Sets.newHashSet(new Timestamped(alty.getId(), TestEnv.LocalNow())));

        String recentAsString = RecentJourneys.encodeCookie(mapper,recentJourneys);
        Optional<Cookie> cookie = Optional.of(new Cookie("tramchesterRecent", recentAsString));

        List<StationDTO> stations = getAll(cookie).getStations();

        stations.removeIf(station -> !station.getId().equals(alty.getId()));
        assertEquals(1, stations.size());
        assertEquals(ProximityGroup.RECENT, stations.get(0).getProximityGroup());

        stations = getNearest(53.4804263d, -2.2392436d, cookie).getStations();

        stations.removeIf(station -> !station.getId().equals(alty.getId()));
        assertEquals(1, stations.size());
        assertEquals(ProximityGroup.RECENT, stations.get(0).getProximityGroup());
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private StationListDTO getNearest(double lat, double lon, Optional<Cookie> cookie) {
        Response result = IntegrationClient.getResponse(testRule, String.format("stations/%s/%s", lat, lon), cookie, 200);
        assertEquals(200,result.getStatus());
        return result.readEntity(StationListDTO.class);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private StationListDTO getAll(Optional<Cookie> cookie) {
        Response result = IntegrationClient.getResponse(testRule, "stations", cookie, 200);
        assertEquals(200,result.getStatus());
        return result.readEntity(StationListDTO.class);
    }

}
