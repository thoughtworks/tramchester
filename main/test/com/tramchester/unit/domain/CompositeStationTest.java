package com.tramchester.unit.domain;

import com.tramchester.domain.Platform;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.CompositeId;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.CompositeStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TestStation;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.tramchester.domain.reference.TransportMode.Bus;
import static com.tramchester.domain.reference.TransportMode.Tram;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompositeStationTest {

    @Test
    void shouldHaveCorrectValuesForOneStation() {
        LatLong latLong = new LatLong(-2.0, 2.3);
        Station stationA = TestStation.forTest("id", "area", "stopName",
                latLong, Tram);

        Route route = TestEnv.getTramTestRoute();
        stationA.addRoute(route);

        Platform platform = new Platform("platformId", "platformName", latLong);
        stationA.addPlatform(platform);

        CompositeStation compositeStation = new CompositeStation(Collections.singleton(stationA), "compArea", "compName");

        assertEquals("compName", compositeStation.getName());
        assertEquals(new CompositeId<Station>(StringIdFor.createId("id")), compositeStation.getId());
        assertEquals(-2.0, compositeStation.getLatLong().getLat(),0);
        assertEquals(2.3, compositeStation.getLatLong().getLon(),0);
        assertEquals("compArea", compositeStation.getArea());

        assertEquals("id", compositeStation.forDTO());

        assertEquals(1, compositeStation.getTransportModes().size());
        assertTrue(compositeStation.getTransportModes().contains(Tram));

        assertTrue(compositeStation.hasPlatforms());
        assertEquals(Collections.singleton(platform), compositeStation.getPlatforms());
        assertEquals(Collections.singleton(route), compositeStation.getRoutes());
        assertEquals(1, compositeStation.getAgencies().size());

        Set<Station> containted = compositeStation.getContained();
        assertEquals(1, containted.size());
        assertTrue(containted.contains(stationA));
    }

    @Test
    void shouldHaveCorrectValuesForTwoStation() {
        Station stationA = TestStation.forTest("idA", "areaA", "stopNameA",
                new LatLong(2, 4), Tram);
        Route routeA = TestEnv.getTramTestRoute(StringIdFor.createId("routeA"));
        stationA.addRoute(routeA);
        Platform platformA = new Platform("platformIdA", "platformNameA",  new LatLong(2, 4));
        stationA.addPlatform(platformA);

        Station stationB = TestStation.forTest("idB", "areaB", "stopNameB",
                new LatLong(4, 8), Bus);
        Route routeB = new Route(StringIdFor.createId("routeB"), "routeCodeB", "routeNameB", TestEnv.StagecoachManchester, Bus);
        stationB.addRoute(routeB);
        stationB.addRoute(routeA);
        Platform platformB = new Platform("platformIdB", "platformNameB",  new LatLong(4, 8));
        stationB.addPlatform(platformB);

        Set<Station> stations = new HashSet<>(Arrays.asList(stationA, stationB));
        CompositeStation compositeStation = new CompositeStation(stations, "compArea", "compName");

        assertEquals("compName", compositeStation.getName());
        CompositeId<Station> expectedId = new CompositeId<Station>(StringIdFor.createId("idB"), StringIdFor.createId("idA"));
        assertEquals(expectedId, compositeStation.getId());
        assertEquals(3, compositeStation.getLatLong().getLat(),0);
        assertEquals(6, compositeStation.getLatLong().getLon(),0);
        assertEquals("compArea", compositeStation.getArea());
        assertEquals(2, compositeStation.getTransportModes().size());
        assertTrue(compositeStation.getTransportModes().contains(Tram));
        assertTrue(compositeStation.getTransportModes().contains(Bus));

        assertEquals(2, compositeStation.getRoutes().size());

        assertEquals(2, compositeStation.getAgencies().size());

        Set<Station> containted = compositeStation.getContained();
        assertEquals(2, containted.size());
        assertTrue(containted.contains(stationA));
        assertTrue(containted.contains(stationB));

    }

}
