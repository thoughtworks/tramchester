package com.tramchester.unit.domain;

import com.tramchester.domain.*;
import com.tramchester.domain.id.CompositeId;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.CompositeStation;
import com.tramchester.domain.places.LocationType;
import com.tramchester.domain.places.MutableStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TestStation;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static com.tramchester.domain.reference.TransportMode.Bus;
import static com.tramchester.domain.reference.TransportMode.Tram;
import static org.junit.jupiter.api.Assertions.*;

class CompositeStationTest {

    private final DataSourceID dataSourceID = DataSourceID.tfgm;

    @Test
    void shouldHaveCorrectValuesForOneStation() {
        LatLong latLong = new LatLong(-2.0, 2.3);
        MutableStation stationA = TestStation.forTest("id", "area", "stopName",
                latLong, Tram, dataSourceID);

        Route route = TestEnv.getTramTestRoute();
        stationA.addRoutePickUp(route);

        Platform platform = MutablePlatform.buildForTFGMTram("platformId", "platformName", latLong);
        stationA.addPlatform(platform);

        CompositeStation compositeStation = new CompositeStation(Collections.singleton(stationA), "compArea",
                "compName",1);

        assertEquals(LocationType.CompositeStation, compositeStation.getLocationType());

        assertEquals("compName", compositeStation.getName());
        IdSet<Station> expected = IdSet.singleton(Station.createId("id"));
        assertEquals(new CompositeId<>(expected), compositeStation.getId());
        assertEquals(-2.0, compositeStation.getLatLong().getLat(),0);
        assertEquals(2.3, compositeStation.getLatLong().getLon(),0);
        assertEquals("compArea", compositeStation.getArea());

        assertEquals("[id]", compositeStation.forDTO());
        assertEquals("[id]", compositeStation.getId().getGraphId());
        assertEquals("[id]", compositeStation.getId().forDTO());

        assertTrue(compositeStation.hasPlatforms());
        assertEquals(Collections.singleton(platform), compositeStation.getPlatforms());
        assertEquals(Collections.singleton(route), compositeStation.getPickupRoutes());

        assertEquals(1, compositeStation.getTransportModes().size());
        assertTrue(compositeStation.servesMode(Tram));

        assertEquals(1, compositeStation.getAgencies().size());

        Set<Station> containted = compositeStation.getContained();
        assertEquals(1, containted.size());
        assertTrue(containted.contains(stationA));
    }

    @Test
    void shouldHaveCorrectValuesForTwoStation() {
        MutableStation stationA = TestStation.forTest("idA", "areaA", "stopNameA",
                new LatLong(2, 4), Tram, dataSourceID);
        Route routeA = TestEnv.getTramTestRoute(StringIdFor.createId("routeA"), "routeName");

        stationA.addRouteDropOff(routeA);
        Platform platformA = MutablePlatform.buildForTFGMTram("platformIdA", "platformNameA",  new LatLong(2, 4));
        stationA.addPlatform(platformA);

        MutableStation stationB = TestStation.forTest("idB", "areaB", "stopNameB",
                new LatLong(4, 8), Bus, dataSourceID);
        Route routeB = MutableRoute.getRoute(StringIdFor.createId("routeB"), "routeCodeB", "routeNameB", TestEnv.StagecoachManchester, Bus);
        stationB.addRouteDropOff(routeB);
        stationB.addRoutePickUp(routeA);
        Platform platformB = MutablePlatform.buildForTFGMTram("platformIdB", "platformNameB",  new LatLong(4, 8));
        stationB.addPlatform(platformB);

        Set<Station> stations = new HashSet<>(Arrays.asList(stationA, stationB));
        CompositeStation compositeStation = new CompositeStation(stations, "compArea", "compName",12);

        assertEquals(LocationType.CompositeStation, compositeStation.getLocationType());

        assertEquals("compName", compositeStation.getName());
        IdSet<Station> expected = Stream.of("idB", "idA").map(Station::createId).collect(IdSet.idCollector());
        assertEquals(new CompositeId<>(expected), compositeStation.getId());
        assertEquals("[idA_idB]", compositeStation.forDTO());
        assertEquals("[idA_idB]", compositeStation.getId().getGraphId());
        assertEquals("[idA_idB]", compositeStation.getId().forDTO());

        assertEquals(3, compositeStation.getLatLong().getLat(),0);
        assertEquals(6, compositeStation.getLatLong().getLon(),0);
        assertEquals("compArea", compositeStation.getArea());
        assertEquals(2, compositeStation.getTransportModes().size());
        assertTrue(compositeStation.servesMode(Tram));
        assertTrue(compositeStation.servesMode(Bus));

        assertEquals(2, compositeStation.getDropoffRoutes().size());
        assertEquals(1, compositeStation.getPickupRoutes().size());
        assertEquals(2, compositeStation.getAgencies().size());

        Set<Station> containted = compositeStation.getContained();
        assertEquals(2, containted.size());
        assertTrue(containted.contains(stationA));
        assertTrue(containted.contains(stationB));

    }

    @Test
    void shouldHaveCorrectPickupAndDropoff() {
        MutableStation stationA = TestStation.forTest("idA", "areaA", "stopNameA",
                new LatLong(2, 4), Tram, dataSourceID);
        Route routeA = TestEnv.getTramTestRoute(StringIdFor.createId("routeA"), "routeName");

        MutableStation stationB = TestStation.forTest("idB", "areaB", "stopNameB",
                new LatLong(4, 8), Bus, dataSourceID);
        Route routeB = MutableRoute.getRoute(StringIdFor.createId("routeB"), "routeCodeB",
                "routeNameB", TestEnv.StagecoachManchester, Bus);

        Set<Station> stations = new HashSet<>(Arrays.asList(stationA, stationB));
        CompositeStation compositeStation = new CompositeStation(stations, "compArea", "compName",11);

        assertFalse(compositeStation.hasPickup());
        assertFalse(compositeStation.hasDropoff());

        stationA.addRouteDropOff(routeA);
        assertTrue(compositeStation.hasDropoff());

        stationB.addRoutePickUp(routeB);
        assertTrue(compositeStation.hasPickup());

    }

    @Test
    void shouldHaveCorrectMinChangeCostForACompositeStation() {
        final LatLong positionA = new LatLong(3, 4);
        final LatLong positionB = new LatLong(2, 5);

        MutableStation stationA = TestStation.forTest("idA", "areaA", "stopNameA",
                positionA, Tram, dataSourceID);

        MutableStation stationB = TestStation.forTest("idB", "areaB", "stopNameB",
                positionB, Bus, dataSourceID);

        CompositeStation compositeStation = new CompositeStation( new HashSet<>(Arrays.asList(stationA, stationB)),
                "compArea", "compName", 42);
;

        assertEquals(42, compositeStation.getMinimumChangeCost());
    }

}
