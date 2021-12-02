package com.tramchester.unit.domain;


import com.tramchester.domain.Agency;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.MutableRoute;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.MutableStation;
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

    @Test
    void testShouldCreateCorrecly() {
        Station tramStation = TestStation.forTest("id", "area", "stopName",
                new LatLong(-2.0, 2.3), Tram, DataSourceID.tfgm);

        assertEquals("stopName", tramStation.getName());
        assertEquals(StringIdFor.createId("id"), tramStation.getId());
        assertEquals(-2.0, tramStation.getLatLong().getLat(),0);
        assertEquals(2.3, tramStation.getLatLong().getLon(),0);
        assertEquals("area", tramStation.getArea());
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
        assertEquals("area", busStation.getArea());
        //assertTrue(TransportMode.isBus(busStation));
    }

    @Test
    void shouldHaveCorrectTransportModes() {
        MutableStation station = new MutableStation(StringIdFor.createId("stationId"), "area", "name", nearPiccGardens,
                CoordinateTransforms.getGridPosition(nearPiccGardens), DataSourceID.tfgm);

        assertTrue(station.getTransportModes().isEmpty());

        final Route route = MutableRoute.getRoute(StringIdFor.createId("routeIdA"), "shortName", "name",
                TestEnv.MetAgency(), Tram);
        station.addRouteDropOff(route);
        assertTrue(station.serves(Tram));

        station.addRouteDropOff(MutableRoute.getRoute(StringIdFor.createId("routeIdB"), "trainShort", "train",
                Walking, Train));
        assertTrue(station.serves(Train));

        assertEquals(2, station.getTransportModes().size());
    }

    @Test
    void shouldHavePickupAndDropoffRoutes() {
        MutableStation station = new MutableStation(StringIdFor.createId("stationId"), "area", "name", nearPiccGardens,
                CoordinateTransforms.getGridPosition(nearPiccGardens), DataSourceID.tfgm);

        final Route routeA = MutableRoute.getRoute(StringIdFor.createId("routeIdA"), "shortNameA", "nameA",
                TestEnv.MetAgency(), Tram);
        final Route routeB = MutableRoute.getRoute(StringIdFor.createId("routeIdB"), "shortNameB", "nameB",
                TestEnv.StagecoachManchester, Bus);

        station.addRoutePickUp(routeA);
        station.addRouteDropOff(routeB);

        assertTrue(station.serves(Tram));
        assertTrue(station.serves(Bus));

        Set<Agency> agencies = station.getAgencies();
        assertEquals(2, agencies.size());
        assertTrue(agencies.contains(TestEnv.MetAgency()));
        assertTrue(agencies.contains(TestEnv.StagecoachManchester));

        Set<Route> allRoutes = station.getRoutes();
        assertEquals(2, allRoutes.size());
        assertTrue(allRoutes.contains(routeA));
        assertTrue(allRoutes.contains(routeB));

        assertTrue(station.servesRoutePickup(routeA));
        assertFalse(station.servesRoutePickup(routeB));

        assertTrue(station.servesRouteDropoff(routeB));
        assertFalse(station.servesRouteDropoff(routeA));

        // TODO Routes for platforms?
    }


}
