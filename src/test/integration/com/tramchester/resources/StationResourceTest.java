package com.tramchester.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.tramchester.Dependencies;
import com.tramchester.IntegrationTramTestConfig;
import com.tramchester.Stations;
import com.tramchester.domain.Station;
import com.tramchester.domain.presentation.DisplayStation;
import org.joda.time.DateTime;
import org.junit.*;

import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StationResourceTest {
    private static Dependencies dependencies;
    private StationResource stationResource;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        dependencies.initialise(new IntegrationTramTestConfig());
    }

    @Before
    public void beforeEachTestRuns() {
        stationResource = dependencies.get(StationResource.class);
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Test
    public void shouldGetNearestStations() throws Exception {
        Response result = stationResource.getNearest(53.4804263d, -2.2392436d);
        List<DisplayStation> stations = (List<DisplayStation>) result.getEntity();

        Map<String, Long> stationGroups = stations.stream()
                .collect(Collectors.groupingBy(o -> o.getProximityGroup(), Collectors.counting()));

        assertTrue(stationGroups.get("Nearest Stops") > 0);
        int ALL_STOPS_START = 7; // 6 + 1
        assertEquals("All Stops", stations.get(ALL_STOPS_START).getProximityGroup());
        assertEquals("Abraham Moss", stations.get(ALL_STOPS_START).getName());
        assertEquals("Altrincham", stations.get(ALL_STOPS_START+1).getName());

        assertThat(stations.stream().filter(station -> station.getName().equals("St Peters Square")).count()).isEqualTo(0);
    }

    @Test
    public void shouldGetSpecialStationWithMyLocation() throws JsonProcessingException {
        Response result = stationResource.getNearest(53.4804263d, -2.2392436d);
        List<DisplayStation> stations = (List<DisplayStation>) result.getEntity();

        DisplayStation station = stations.get(0);
        assertEquals("Nearby", station.getProximityGroup());
        assertEquals("{\"lat\":53.4804263,\"lon\":-2.2392436}", station.getId());
        assertEquals("My Location", station.getName());
        assertEquals("Nearest Stops", stations.get(1).getProximityGroup());
    }

    @Test
    @Ignore("For performande testing")
    public void shouldCheckPerformanceOfGettingNearest() throws JsonProcessingException {
        DateTime start = DateTime.now();
        for (int i=0; i<10000; i++) {
            stationResource.getNearest(53.4804263d, -2.2392436d);
        }
        DateTime finished = DateTime.now();

        System.out.println("Initialisation took: " + finished.minus(start.getMillis()).getMillis());
    }

    @Test
    public void shouldNotGetClosedStations() throws Exception {
        Response result = stationResource.getAll();
        Collection<Station> stations = (Collection<Station>) result.getEntity();

        assertThat(stations.stream().filter(station -> station.getName().equals("St Peters Square")).count()).isEqualTo(0);
        assertThat(stations.stream().filter(station -> station.getName().equals(Stations.Altrincham.getName())).count()).isEqualTo(1);
    }

    @Test
    public void shouldGetAllStationsWithRightOrderAndProxGroup() {
        Response result = stationResource.getAll();
        Collection<DisplayStation> stations = (Collection<DisplayStation>) result.getEntity();

        assertThat(stations.stream().findFirst().get().getName()).isEqualTo("Abraham Moss");

        stations.forEach(station -> assertThat(station.getProximityGroup()).isEqualTo("All Stops"));

    }
}
