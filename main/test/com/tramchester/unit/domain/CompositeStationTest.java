package com.tramchester.unit.domain;

import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.Platform;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.CompositeId;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.CompositeStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationBuilder;
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

    private final DataSourceID dataSourceID = DataSourceID.tfgm;

    @Test
    void shouldHaveCorrectValuesForOneStation() {
        LatLong latLong = new LatLong(-2.0, 2.3);
        Station stationA = TestStation.forTest("id", "area", "stopName",
                latLong, Tram, dataSourceID);
        final StationBuilder stationABuilder = stationA.getBuilder();

        Route route = TestEnv.getTramTestRoute();
        stationABuilder.addRoute(route);

        Platform platform = new Platform("platformId", "platformName", latLong);
        stationABuilder.addPlatform(platform);

        CompositeStation compositeStation = new CompositeStation(Collections.singleton(stationA), "compArea", "compName");

        assertEquals("compName", compositeStation.getName());
        assertEquals(new CompositeId<Station>(StringIdFor.createId("id")), compositeStation.getId());
        assertEquals(-2.0, compositeStation.getLatLong().getLat(),0);
        assertEquals(2.3, compositeStation.getLatLong().getLon(),0);
        assertEquals("compArea", compositeStation.getArea());

        assertEquals("[id]", compositeStation.forDTO());
        assertEquals("[id]", compositeStation.getId().getGraphId());
        assertEquals("[id]", compositeStation.getId().forDTO());

        assertTrue(compositeStation.hasPlatforms());
        assertEquals(Collections.singleton(platform), compositeStation.getPlatforms());
        assertEquals(Collections.singleton(route), compositeStation.getRoutes());

        assertEquals(1, compositeStation.getTransportModes().size());
        assertTrue(compositeStation.serves(Tram));

        assertEquals(1, compositeStation.getAgencies().size());

        Set<Station> containted = compositeStation.getContained();
        assertEquals(1, containted.size());
        assertTrue(containted.contains(stationA));
    }

    @Test
    void shouldHaveCorrectValuesForTwoStation() {
        Station stationA = TestStation.forTest("idA", "areaA", "stopNameA",
                new LatLong(2, 4), Tram, dataSourceID);
        Route routeA = TestEnv.getTramTestRoute(StringIdFor.createId("routeA"), "routeName");
        final StationBuilder stationABuilder = stationA.getBuilder();
        stationABuilder.addRoute(routeA);
        Platform platformA = new Platform("platformIdA", "platformNameA",  new LatLong(2, 4));
        stationABuilder.addPlatform(platformA);

        Station stationB = TestStation.forTest("idB", "areaB", "stopNameB",
                new LatLong(4, 8), Bus, dataSourceID);
        Route routeB = new Route(StringIdFor.createId("routeB"), "routeCodeB", "routeNameB", TestEnv.StagecoachManchester, Bus);
        final StationBuilder stationBBuilder = stationB.getBuilder();
        stationBBuilder.addRoute(routeB);
        stationBBuilder.addRoute(routeA);
        Platform platformB = new Platform("platformIdB", "platformNameB",  new LatLong(4, 8));
        stationBBuilder.addPlatform(platformB);

        Set<Station> stations = new HashSet<>(Arrays.asList(stationA, stationB));
        CompositeStation compositeStation = new CompositeStation(stations, "compArea", "compName");

        assertEquals("compName", compositeStation.getName());
        CompositeId<Station> expectedId = new CompositeId<>(StringIdFor.createId("idB"), StringIdFor.createId("idA"));
        assertEquals(expectedId, compositeStation.getId());
        assertEquals("[idA_idB]", compositeStation.forDTO());
        assertEquals("[idA_idB]", compositeStation.getId().getGraphId());
        assertEquals("[idA_idB]", compositeStation.getId().forDTO());

        assertEquals(3, compositeStation.getLatLong().getLat(),0);
        assertEquals(6, compositeStation.getLatLong().getLon(),0);
        assertEquals("compArea", compositeStation.getArea());
        assertEquals(2, compositeStation.getTransportModes().size());
        assertTrue(compositeStation.serves(Tram));
        assertTrue(compositeStation.serves(Bus));

        assertEquals(2, compositeStation.getRoutes().size());
        assertEquals(2, compositeStation.getAgencies().size());

        Set<Station> containted = compositeStation.getContained();
        assertEquals(2, containted.size());
        assertTrue(containted.contains(stationA));
        assertTrue(containted.contains(stationB));

    }

}
