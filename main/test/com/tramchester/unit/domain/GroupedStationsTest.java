package com.tramchester.unit.domain;

import com.tramchester.domain.*;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.*;
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
import static com.tramchester.integration.testSupport.Assertions.assertIdEquals;
import static org.junit.jupiter.api.Assertions.*;

class GroupedStationsTest {

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

        IdFor<NaptanArea> areaId = StringIdFor.createId("areaId");
        GroupedStations groupedStations = new GroupedStations(Collections.singleton(stationA), areaId,
                "compName",1);

        assertEquals(LocationType.StationGroup, groupedStations.getLocationType());

        assertEquals("compName", groupedStations.getName());
        assertIdEquals("areaId", groupedStations.getId());
        assertEquals(-2.0, groupedStations.getLatLong().getLat(),0);
        assertEquals(2.3, groupedStations.getLatLong().getLon(),0);

        assertEquals(areaId, groupedStations.getAreaId());
        assertEquals("areaId", groupedStations.forDTO());
        assertEquals("areaId", groupedStations.getId().getGraphId());
        assertEquals("areaId", groupedStations.getId().forDTO());

        assertTrue(groupedStations.hasPlatforms());
        assertEquals(Collections.singleton(platform), groupedStations.getPlatforms());
        assertEquals(Collections.singleton(route), groupedStations.getPickupRoutes());

        assertEquals(1, groupedStations.getTransportModes().size());
        assertTrue(groupedStations.servesMode(Tram));

        assertEquals(1, groupedStations.getAgencies().size());

        Set<Station> containted = groupedStations.getContained();
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
        IdFor<NaptanArea> areaId = StringIdFor.createId("areaId");
        GroupedStations groupedStations = new GroupedStations(stations, areaId, "compName",12);

        assertEquals(LocationType.StationGroup, groupedStations.getLocationType());

        assertEquals("compName", groupedStations.getName());
        IdSet<Station> expected = Stream.of("idB", "idA").map(Station::createId).collect(IdSet.idCollector());
        assertIdEquals("areaId", groupedStations.getId());
        assertEquals("areaId", groupedStations.forDTO());
        assertEquals("areaId", groupedStations.getId().getGraphId());
        assertEquals("areaId", groupedStations.getId().forDTO());

        assertEquals(3, groupedStations.getLatLong().getLat(),0);
        assertEquals(6, groupedStations.getLatLong().getLon(),0);
        assertEquals(areaId, groupedStations.getAreaId());

        assertEquals(2, groupedStations.getTransportModes().size());
        assertTrue(groupedStations.servesMode(Tram));
        assertTrue(groupedStations.servesMode(Bus));

        assertEquals(2, groupedStations.getDropoffRoutes().size());
        assertEquals(1, groupedStations.getPickupRoutes().size());
        assertEquals(2, groupedStations.getAgencies().size());

        Set<Station> containted = groupedStations.getContained();
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
        IdFor<NaptanArea> areaId = StringIdFor.createId("areaId");
        GroupedStations groupedStations = new GroupedStations(stations, areaId, "compName",11);

        assertFalse(groupedStations.hasPickup());
        assertFalse(groupedStations.hasDropoff());

        stationA.addRouteDropOff(routeA);
        assertTrue(groupedStations.hasDropoff());

        stationB.addRoutePickUp(routeB);
        assertTrue(groupedStations.hasPickup());

    }

    @Test
    void shouldHaveCorrectMinChangeCostForACompositeStation() {
        final LatLong positionA = new LatLong(3, 4);
        final LatLong positionB = new LatLong(2, 5);

        MutableStation stationA = TestStation.forTest("idA", "areaA", "stopNameA",
                positionA, Tram, dataSourceID);

        MutableStation stationB = TestStation.forTest("idB", "areaB", "stopNameB",
                positionB, Bus, dataSourceID);

        IdFor<NaptanArea> areaId = StringIdFor.createId("areaId");

        GroupedStations groupedStations = new GroupedStations( new HashSet<>(Arrays.asList(stationA, stationB)),
                areaId, "compName", 42);


        assertEquals(42, groupedStations.getMinimumChangeCost());
    }

}
