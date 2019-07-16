package com.tramchester.integration.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.google.common.collect.Sets;
import com.tramchester.App;
import com.tramchester.LiveDataMessagesCategory;
import com.tramchester.LiveDataTestCategory;
import com.tramchester.domain.Location;
import com.tramchester.domain.Timestamped;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.domain.presentation.DTO.PlatformDTO;
import com.tramchester.domain.presentation.DTO.StationDTO;
import com.tramchester.domain.presentation.DTO.StationListDTO;
import com.tramchester.domain.presentation.ProximityGroup;
import com.tramchester.domain.presentation.RecentJourneys;
import com.tramchester.integration.IntegrationClient;
import com.tramchester.integration.IntegrationTestRun;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.integration.Stations;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static junit.framework.TestCase.assertEquals;
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
    public void shouldGetSingleStationWithPlatforms() {
        String id = Stations.StPetersSquare.getId();
        String endPoint = "stations/" + id;
        Response response = IntegrationClient.getResponse(testRule, endPoint, Optional.empty());
        assertEquals(200,response.getStatus());
        StationDTO result = response.readEntity(StationDTO.class);

        assertEquals(id, result.getId());

        List<PlatformDTO> platforms = result.getPlatforms();
        assertEquals(4, platforms.size());
        assertEquals(id+"1", platforms.get(0).getId());
        assertEquals(id+"2", platforms.get(1).getId());
        assertEquals(id+"3", platforms.get(2).getId());
        assertEquals(id+"4", platforms.get(3).getId());
    }

    @Test
    @Category(LiveDataTestCategory.class)
    public void shouldGetSingleStationWithLiveData() {
        String id = Stations.StPetersSquare.getId();
        String endPoint = "stations/live/" + id;
        Response response = IntegrationClient.getResponse(testRule, endPoint, Optional.empty());
        assertEquals(200,response.getStatus());
        StationDTO result = response.readEntity(StationDTO.class);

        assertEquals(id, result.getId());

        List<PlatformDTO> platforms = result.getPlatforms();
        assertEquals(4, platforms.size());
        StationDepartureInfo info = platforms.get(0).getStationDepartureInfo();
        assertNotNull(info);
        assertEquals("St Peter's Square", info.getLocation());
    }

    @Test
    public void shouldGetNearestStations() {
        List<StationDTO> stations = getNearest(53.4804263d, -2.2392436d, Optional.empty()).getStations();

        Map<ProximityGroup, Long> stationGroups = stations.stream()
                .collect(Collectors.groupingBy(o -> o.getProximityGroup(), Collectors.counting()));

        assertTrue(stationGroups.get(ProximityGroup.NEAREST_STOPS) > 0);
        int ALL_STOPS_START = 7; // 6 + 1
        assertEquals(ProximityGroup.ALL, stations.get(ALL_STOPS_START).getProximityGroup());
        assertEquals("Abraham Moss", stations.get(ALL_STOPS_START).getName());
        StationDTO stationDTO = stations.get(ALL_STOPS_START + 1);
        assertEquals("Altrincham", stationDTO.getName());

        List<PlatformDTO> platforms = stationDTO.getPlatforms();
        assertEquals(1, platforms.size());
        PlatformDTO platformDTO = platforms.get(0);
        assertEquals("1", platformDTO.getPlatformNumber());
        assertEquals("Altrincham platform 1", platformDTO.getName());
        assertEquals(Stations.Altrincham.getId()+"1", platformDTO.getId());
    }

    @Test
    @Category({LiveDataTestCategory.class, LiveDataMessagesCategory.class})
    public void shouldGetNearestStationsWithLiveData() {
        double lat = 53.4804263d;
        double lon = -2.2392436d;

        Response response = IntegrationClient.getResponse(testRule, String.format("stations/live/%s/%s", lat, lon),
                Optional.empty());
        assertEquals(200,response.getStatus());

        StationListDTO stationList =  response.readEntity(StationListDTO.class);
        List<StationDTO> stations = stationList.getStations();

        assertEquals(6, stations.size());

        stations.forEach(stationDTO -> {
            assertTrue(stationDTO.getName(), stationDTO.hasPlatforms());
            stationDTO.getPlatforms().forEach(platformDTO -> {
                StationDepartureInfo info = platformDTO.getStationDepartureInfo();
                assertNotNull(stationDTO.getName(), info);
            });
        });

        List<String> notes = stationList.getNotes();
        assertFalse(notes.isEmpty());
        // ignore closure message which is always present, also if today is weekend exclude that
        int ignore = 1;
        DayOfWeek dayOfWeek = LocalDate.now().getDayOfWeek();
        if (dayOfWeek.equals(DayOfWeek.SATURDAY) || dayOfWeek.equals(DayOfWeek.SUNDAY)) {
            ignore++;
        }
        assertTrue((notes.size())-ignore>0);
    }

    @Test
    public void shouldGetSpecialStationWithMyLocation() {
        List<StationDTO> stations = getNearest(53.4804263d, -2.2392436d, Optional.empty()).getStations();

        StationDTO station = stations.get(0);
        assertEquals(ProximityGroup.MY_LOCATION, station.getProximityGroup());
        assertEquals("{\"lat\":53.4804263,\"lon\":-2.2392436}", station.getId());
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
        Collection<StationDTO> stations = getAll(Optional.empty()).getStations();

        assertThat(stations.stream().findFirst().get().getName()).isEqualTo("Abraham Moss");
        stations.forEach(station -> assertThat(station.getProximityGroup()).isEqualTo(ProximityGroup.ALL));
    }

    @Test
    public void shouldReturnRecentStationsGroupIfCookieSet() throws JsonProcessingException, UnsupportedEncodingException {
        Location alty = Stations.Altrincham;
        RecentJourneys recentJourneys = new RecentJourneys();
        recentJourneys.setTimestamps(Sets.newHashSet(new Timestamped(alty.getId(), LocalDateTime.now())));

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

    private StationListDTO getNearest(double lat, double lon, Optional<Cookie> cookie) {
        Response result = IntegrationClient.getResponse(testRule, String.format("stations/%s/%s", lat, lon), cookie);
        assertEquals(200,result.getStatus());
        return result.readEntity(StationListDTO.class);
    }

    private StationListDTO getAll(Optional<Cookie> cookie) {
        Response result = IntegrationClient.getResponse(testRule, "stations", cookie);
        assertEquals(200,result.getStatus());
        return result.readEntity(StationListDTO.class);
    }

}
