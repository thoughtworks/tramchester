package com.tramchester.resources;

import com.tramchester.Dependencies;
import com.tramchester.IntegrationTramTestConfig;
import com.tramchester.domain.Station;
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

public class StationsTest {
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
        List<Station> stations = (List<Station>) result.getEntity();

        Map<String, Long> stationGroups = stations.stream()
                .collect(Collectors.groupingBy(o -> o.getProximityGroup(), Collectors.counting()));

        assertTrue(stationGroups.get("Nearest Stops") > 0);
        assertEquals("All Stops", stations.get(6).getProximityGroup());
        assertEquals("Abraham Moss", stations.get(6).getName());
        assertEquals("Altrincham", stations.get(7).getName());

        assertThat(stations.stream().filter(station -> station.getName().equals("St Peters Square")).count()).isEqualTo(0);
    }

    @Test
    @Ignore("For performande testing")
    public void shouldCheckPerformanceOfGettingNearest() {
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
        assertThat(stations.stream().filter(station -> station.getName().equals("Altrincham")).count()).isEqualTo(1);
    }
}
