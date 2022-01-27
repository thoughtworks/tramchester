package com.tramchester.unit.domain;


import com.tramchester.domain.Agency;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.MutableRoute;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.MutableStation;
import com.tramchester.domain.places.NaptanArea;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TestStation;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.tramchester.domain.MutableAgency.Walking;
import static com.tramchester.domain.reference.TransportMode.*;
import static com.tramchester.testSupport.TestEnv.nearPiccGardens;
import static org.junit.jupiter.api.Assertions.*;

class StationTest {

    private final IdFor<NaptanArea> areaId = StringIdFor.createId("area");

    @Test
    void testShouldCreateCorrecly() {
        Station tramStation = TestStation.forTest("id", "area", "stopName",
                new LatLong(-2.0, 2.3), Tram, DataSourceID.tfgm);

        assertEquals("stopName", tramStation.getName());
        assertEquals(StringIdFor.createId("id"), tramStation.getId());
        assertEquals(-2.0, tramStation.getLatLong().getLat(),0);
        assertEquals(2.3, tramStation.getLatLong().getLon(),0);
        assertEquals(areaId, tramStation.getAreaId());
        assertEquals(DataSourceID.tfgm, tramStation.getDataSourceID());
    }

    @Test
    void testShouldSetBusNameCorrecly() {
        Station busStation = TestStation.forTest("id", "area", "stopName",
                new LatLong(-2.0, 2.3), Bus, DataSourceID.tfgm);

        assertEquals("stopName", busStation.getName());
        assertEquals(StringIdFor.createId("id"), busStation.getId());
        assertEquals(-2.0, busStation.getLatLong().getLat(),0);
        assertEquals(2.3, busStation.getLatLong().getLon(),0);
        assertEquals(areaId, busStation.getAreaId());
        //assertTrue(TransportMode.isBus(busStation));
    }

    @Test
    void shouldHaveCorrectTransportModes() {
        IdFor<NaptanArea> areaId = IdFor.invalid();
        MutableStation station = new MutableStation(StringIdFor.createId("stationId"), "area", areaId, "name", nearPiccGardens,
                CoordinateTransforms.getGridPosition(nearPiccGardens), DataSourceID.tfgm);

        assertTrue(station.getTransportModes().isEmpty());

        final Route route = MutableRoute.getRoute(StringIdFor.createId("routeIdA"), "shortName", "name",
                TestEnv.MetAgency(), Tram);
        station.addRouteDropOff(route);
        assertTrue(station.servesMode(Tram));

        station.addRouteDropOff(MutableRoute.getRoute(StringIdFor.createId("routeIdB"), "trainShort", "train",
                Walking, Train));
        assertTrue(station.servesMode(Train));

        assertEquals(2, station.getTransportModes().size());
    }

    @Test
    void shouldHavePickupAndDropoffRoutes() {
        IdFor<NaptanArea> areaId = IdFor.invalid();
        MutableStation station = new MutableStation(StringIdFor.createId("stationId"), "area", areaId, "name", nearPiccGardens,
                CoordinateTransforms.getGridPosition(nearPiccGardens), DataSourceID.tfgm);

        final Route routeA = MutableRoute.getRoute(StringIdFor.createId("routeIdA"), "shortNameA", "nameA",
                TestEnv.MetAgency(), Tram);
        final Route routeB = MutableRoute.getRoute(StringIdFor.createId("routeIdB"), "shortNameB", "nameB",
                TestEnv.StagecoachManchester, Bus);

        assertFalse(station.hasPickup());
        assertFalse(station.hasDropoff());

        station.addRoutePickUp(routeA);
        assertTrue(station.hasPickup());

        station.addRouteDropOff(routeB);
        assertTrue(station.hasDropoff());

        assertTrue(station.servesMode(Tram));
        assertTrue(station.servesMode(Bus));

        Set<Agency> agencies = station.getAgencies();
        assertEquals(2, agencies.size());
        assertTrue(agencies.contains(TestEnv.MetAgency()));
        assertTrue(agencies.contains(TestEnv.StagecoachManchester));

        Set<Route> dropOffRoutes = station.getDropoffRoutes();
        assertEquals(1, dropOffRoutes.size());
        assertTrue(dropOffRoutes.contains(routeB));

        Set<Route> pickupRoutes = station.getPickupRoutes();
        assertEquals(1, pickupRoutes.size());
        assertTrue(pickupRoutes.contains(routeA));

        assertTrue(station.servesRoutePickup(routeA));
        assertFalse(station.servesRoutePickup(routeB));

        assertTrue(station.servesRouteDropoff(routeB));
        assertFalse(station.servesRouteDropoff(routeA));

        // TODO Routes for platforms?
    }


}
