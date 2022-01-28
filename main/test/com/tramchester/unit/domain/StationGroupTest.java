package com.tramchester.unit.domain;

import com.tramchester.domain.*;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.*;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.StationHelper;
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

class StationGroupTest {

    private final DataSourceID dataSourceID = DataSourceID.tfgm;

    @Test
    void shouldHaveCorrectValuesForOneStation() {
        LatLong latLong = new LatLong(-2.0, 2.3);
        MutableStation stationA = StationHelper.forTestMutable("id", "area", "stopName",
                latLong, dataSourceID);

        Route route = TestEnv.getTramTestRoute();
        stationA.addRoutePickUp(route);

        Platform platform = MutablePlatform.buildForTFGMTram("platformId", "platformName", latLong);
        stationA.addPlatform(platform);

        IdFor<NaptanArea> areaId = StringIdFor.createId("areaId");
        StationGroup groupedStations = new StationGroup(Collections.singleton(stationA), areaId,
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

        final Set<TransportMode> transportModes = groupedStations.getTransportModes();
        assertEquals(1, transportModes.size());
        assertTrue(transportModes.contains(Tram));

        Set<Station> containted = groupedStations.getContained();
        assertEquals(1, containted.size());
        assertTrue(containted.contains(stationA));
    }

    @Test
    void shouldHaveCorrectValuesForTwoStation() {
        MutableStation stationA = StationHelper.forTestMutable("idA", "areaA", "stopNameA",
                new LatLong(2, 4), dataSourceID);
        Route routeA = TestEnv.getTramTestRoute(StringIdFor.createId("routeA"), "routeName");

        stationA.addRouteDropOff(routeA);
        Platform platformA = MutablePlatform.buildForTFGMTram("platformIdA", "platformNameA",  new LatLong(2, 4));
        stationA.addPlatform(platformA);

        MutableStation stationB = StationHelper.forTestMutable("idB", "areaB", "stopNameB",
                new LatLong(4, 8), dataSourceID);
        Route routeB = MutableRoute.getRoute(StringIdFor.createId("routeB"), "routeCodeB", "routeNameB", TestEnv.StagecoachManchester, Bus);
        stationB.addRouteDropOff(routeB);
        stationB.addRoutePickUp(routeA);
        Platform platformB = MutablePlatform.buildForTFGMTram("platformIdB", "platformNameB",  new LatLong(4, 8));
        stationB.addPlatform(platformB);

        Set<Station> stations = new HashSet<>(Arrays.asList(stationA, stationB));
        IdFor<NaptanArea> areaId = StringIdFor.createId("areaId");
        StationGroup stationGroup = new StationGroup(stations, areaId, "compName",12);

        assertEquals(LocationType.StationGroup, stationGroup.getLocationType());

        assertEquals("compName", stationGroup.getName());
        IdSet<Station> expected = Stream.of("idB", "idA").map(Station::createId).collect(IdSet.idCollector());
        assertIdEquals("areaId", stationGroup.getId());
        assertEquals("areaId", stationGroup.forDTO());
        assertEquals("areaId", stationGroup.getId().getGraphId());
        assertEquals("areaId", stationGroup.getId().forDTO());

        assertEquals(3, stationGroup.getLatLong().getLat(),0);
        assertEquals(6, stationGroup.getLatLong().getLon(),0);
        assertEquals(areaId, stationGroup.getAreaId());

        final Set<TransportMode> transportModes = stationGroup.getTransportModes();
        assertEquals(2, transportModes.size());
        assertTrue(transportModes.contains(Tram));
        assertTrue(transportModes.contains(Bus));

        assertEquals(2, stationGroup.getDropoffRoutes().size());
        assertEquals(1, stationGroup.getPickupRoutes().size());
//        assertEquals(2, stationGroup.getAgencies().size());

        Set<Station> containted = stationGroup.getContained();
        assertEquals(2, containted.size());
        assertTrue(containted.contains(stationA));
        assertTrue(containted.contains(stationB));

    }

    @Test
    void shouldHaveCorrectPickupAndDropoff() {
        MutableStation stationA = StationHelper.forTestMutable("idA", "areaA", "stopNameA",
                new LatLong(2, 4), dataSourceID);
        Route routeA = TestEnv.getTramTestRoute(StringIdFor.createId("routeA"), "routeName");

        MutableStation stationB = StationHelper.forTestMutable("idB", "areaB", "stopNameB",
                new LatLong(4, 8), dataSourceID);
        Route routeB = MutableRoute.getRoute(StringIdFor.createId("routeB"), "routeCodeB",
                "routeNameB", TestEnv.StagecoachManchester, Bus);

        Set<Station> stations = new HashSet<>(Arrays.asList(stationA, stationB));
        IdFor<NaptanArea> areaId = StringIdFor.createId("areaId");
        StationGroup stationGroup = new StationGroup(stations, areaId, "compName",11);

        assertFalse(stationGroup.hasPickup());
        assertFalse(stationGroup.hasDropoff());

        stationA.addRouteDropOff(routeA);
        assertTrue(stationGroup.hasDropoff());

        stationB.addRoutePickUp(routeB);
        assertTrue(stationGroup.hasPickup());

    }

    @Test
    void shouldHaveCorrectMinChangeCostForACompositeStation() {
        final LatLong positionA = new LatLong(3, 4);
        final LatLong positionB = new LatLong(2, 5);

        Station stationA = StationHelper.forTest("idA", "areaA", "stopNameA",
                positionA, dataSourceID);

        Station stationB = StationHelper.forTest("idB", "areaB", "stopNameB",
                positionB, dataSourceID);

        IdFor<NaptanArea> areaId = StringIdFor.createId("areaId");

        StationGroup stationGroup = new StationGroup( new HashSet<>(Arrays.asList(stationA, stationB)),
                areaId, "compName", 42);


        assertEquals(42, stationGroup.getMinimumChangeCost());
    }

}
