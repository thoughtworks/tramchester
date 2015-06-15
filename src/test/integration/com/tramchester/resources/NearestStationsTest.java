package com.tramchester.resources;

import com.tramchester.Dependencies;
import com.tramchester.IntegrationTestConfig;
import com.tramchester.domain.Station;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;

public class NearestStationsTest {
    private static Dependencies dependencies;
    private StationResource stationResource;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        dependencies.initialise(new IntegrationTestConfig());
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
    }
}
