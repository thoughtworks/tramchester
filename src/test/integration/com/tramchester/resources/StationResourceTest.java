package com.tramchester.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.tramchester.*;
import com.tramchester.domain.Location;
import com.tramchester.domain.presentation.DisplayStation;
import org.junit.*;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

public class StationResourceTest {

    @ClassRule
    public static IntegrationTestRun testRule = new IntegrationTestRun(App.class, new IntegrationTramTestConfig());

    @Test
    public void shouldGetNearestStations() throws Exception {
        List<DisplayStation> stations = getNearest(53.4804263d, -2.2392436d);

        Map<String, Long> stationGroups = stations.stream()
                .collect(Collectors.groupingBy(o -> o.getProximityGroup(), Collectors.counting()));

        assertTrue(stationGroups.get("Nearest Stops") > 0);
        int ALL_STOPS_START = 7; // 6 + 1
        assertEquals("All Stops", stations.get(ALL_STOPS_START).getProximityGroup());
        assertEquals("Abraham Moss", stations.get(ALL_STOPS_START).getName());
        assertEquals("Altrincham", stations.get(ALL_STOPS_START+1).getName());

    }

    @Test
    public void shouldGetSpecialStationWithMyLocation() throws JsonProcessingException {
        List<DisplayStation> stations = getNearest(53.4804263d, -2.2392436d);

        DisplayStation station = stations.get(0);
        assertEquals("Nearby", station.getProximityGroup());
        assertEquals("{\"lat\":53.4804263,\"lon\":-2.2392436}", station.getId());
        assertEquals("My Location", station.getName());
        // the nearest stops show come next
        assertEquals("Nearest Stops", stations.get(1).getProximityGroup());
    }

    @Test
    public void shouldNotGetSpecialStationWhenGettingAllStations() throws JsonProcessingException {
        getNearest(53.4804263d, -2.2392436d);
        Collection<DisplayStation> stations = getAll();

        stations.forEach(station -> assertNotEquals("My Location", station.getName()));
    }

    @Test
    public void shouldNotGetClosedStations() throws Exception {
        Collection<DisplayStation> stations = getAll();

        assertThat(stations.stream().filter(station -> station.getName().equals("St Peters Square")).count()).isEqualTo(0);
        assertThat(stations.stream().filter(station -> station.getName().equals(Stations.Altrincham.getName())).count()).isEqualTo(1);
    }

    @Test
    public void shouldGetAllStationsWithRightOrderAndProxGroup() {
        Collection<DisplayStation> stations = getAll();

        assertThat(stations.stream().findFirst().get().getName()).isEqualTo("Abraham Moss");

        stations.forEach(station -> assertThat(station.getProximityGroup()).isEqualTo("All Stops"));
    }

    private List<DisplayStation> getNearest(double lat, double lon) {
        Response result = IntegrationClient.getResponse(testRule, String.format("stations/%s/%s", lat, lon));
        return result.readEntity(new GenericType<List<DisplayStation>>(){});
    }

    private List<DisplayStation> getAll() {
        Response result = IntegrationClient.getResponse(testRule, "stations");
        return result.readEntity(new GenericType<ArrayList<DisplayStation>>(){});
    }

}
