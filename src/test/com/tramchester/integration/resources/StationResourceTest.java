package com.tramchester.integration.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.google.common.collect.Sets;
import com.tramchester.App;
import com.tramchester.domain.Location;
import com.tramchester.domain.presentation.RecentJourneys;
import com.tramchester.domain.Timestamped;
import com.tramchester.domain.presentation.DTO.StationDTO;
import com.tramchester.domain.presentation.ProximityGroup;
import com.tramchester.integration.IntegrationClient;
import com.tramchester.integration.IntegrationTestRun;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.integration.Stations;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

public class StationResourceTest {

    @ClassRule
    public static IntegrationTestRun testRule = new IntegrationTestRun(App.class, new IntegrationTramTestConfig());

    ObjectMapper mapper = new ObjectMapper();

    @Before
    public void beforeEachTestRuns() {
        mapper.registerModule(new JodaModule());
    }

    @Test
    public void shouldGetNearestStations() throws Exception {
        List<StationDTO> stations = getNearest(53.4804263d, -2.2392436d, Optional.empty());

        Map<ProximityGroup, Long> stationGroups = stations.stream()
                .collect(Collectors.groupingBy(o -> o.getProximityGroup(), Collectors.counting()));

        assertTrue(stationGroups.get(ProximityGroup.NEAREST_STOPS) > 0);
        int ALL_STOPS_START = 7; // 6 + 1
        assertEquals(ProximityGroup.ALL, stations.get(ALL_STOPS_START).getProximityGroup());
        assertEquals("Abraham Moss", stations.get(ALL_STOPS_START).getName());
        assertEquals("Altrincham", stations.get(ALL_STOPS_START+1).getName());

    }

    @Test
    public void shouldGetSpecialStationWithMyLocation() throws JsonProcessingException {
        List<StationDTO> stations = getNearest(53.4804263d, -2.2392436d, Optional.empty());

        StationDTO station = stations.get(0);
        assertEquals(ProximityGroup.MY_LOCATION, station.getProximityGroup());
        assertEquals("{\"lat\":53.4804263,\"lon\":-2.2392436}", station.getId());
        assertEquals("My Location", station.getName());
        // the nearest stops show come next
        assertEquals(ProximityGroup.NEAREST_STOPS, stations.get(1).getProximityGroup());
    }

    @Test
    public void shouldNotGetSpecialStationWhenGettingAllStations() throws JsonProcessingException {
        getNearest(53.4804263d, -2.2392436d, Optional.empty());
        Collection<StationDTO> stations = getAll(Optional.empty());

        stations.forEach(station -> assertNotEquals("My Location", station.getName()));
    }

    @Test
    public void shouldNotGetClosedStations() throws Exception {
        Collection<StationDTO> stations = getAll(Optional.empty());

        assertThat(stations.stream().filter(station -> station.getName().equals("St Peters Square")).count()).isEqualTo(0);
        assertThat(stations.stream().filter(station -> station.getName().equals(Stations.Altrincham.getName())).count()).isEqualTo(1);
    }

    @Test
    public void shouldGetAllStationsWithRightOrderAndProxGroup() {
        Collection<StationDTO> stations = getAll(Optional.empty());

        assertThat(stations.stream().findFirst().get().getName()).isEqualTo("Abraham Moss");

        stations.forEach(station -> assertThat(station.getProximityGroup()).isEqualTo(ProximityGroup.ALL));
    }

    @Test
    public void shouldReturnRecentStationsGroupIfCookieSet() throws JsonProcessingException, UnsupportedEncodingException {
        Location alty = Stations.Altrincham;
        RecentJourneys recentJourneys = new RecentJourneys();
        recentJourneys.setTimestamps(Sets.newHashSet(new Timestamped(alty.getId(), DateTime.now())));

        String recentAsString = RecentJourneys.encodeCookie(mapper,recentJourneys);
        Optional<Cookie> cookie = Optional.of(new Cookie("tramchesterRecent", recentAsString));

        List<StationDTO> stations = getAll(cookie);

        stations.removeIf(station -> !station.getId().equals(alty.getId()));
        assertEquals(1, stations.size());
        assertEquals(ProximityGroup.RECENT, stations.get(0).getProximityGroup());

        stations = getNearest(53.4804263d, -2.2392436d, cookie);

        stations.removeIf(station -> !station.getId().equals(alty.getId()));
        assertEquals(1, stations.size());
        assertEquals(ProximityGroup.RECENT, stations.get(0).getProximityGroup());
    }

    private List<StationDTO> getNearest(double lat, double lon, Optional<Cookie> cookie) {
        Response result = IntegrationClient.getResponse(testRule, String.format("stations/%s/%s", lat, lon), cookie);
        return result.readEntity(new GenericType<List<StationDTO>>(){});
    }

    private List<StationDTO> getAll(Optional<Cookie> cookie) {
        Response result = IntegrationClient.getResponse(testRule, "stations", cookie);
        return result.readEntity(new GenericType<ArrayList<StationDTO>>(){});
    }

}
