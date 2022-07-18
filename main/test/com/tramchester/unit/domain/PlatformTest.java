package com.tramchester.unit.domain;

import com.tramchester.domain.*;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.NaptanArea;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.tramchester.testSupport.reference.KnownLocations.nearAltrincham;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlatformTest {

    @Test
    void shouldCreatePlatformCorrectly() {
        IdFor<NaptanArea> areaId = StringIdFor.createId("area55");
        boolean isMarkedInterchange = true;
        MutablePlatform platform = new MutablePlatform(StringIdFor.createId("9400ZZ_Name2"),
                TramStations.Altrincham.fake(), "StationName", DataSourceID.tfgm, "2",
                areaId, nearAltrincham.latLong(), nearAltrincham.grid(), isMarkedInterchange);

        assertEquals("StationName platform 2", platform.getName());
        assertEquals(StringIdFor.createId("9400ZZ_Name2"), platform.getId());
        assertEquals( "2", platform.getPlatformNumber());
        assertEquals(nearAltrincham.latLong(), platform.getLatLong());
        assertEquals(nearAltrincham.grid(), platform.getGridPosition());
        assertEquals(DataSourceID.tfgm, platform.getDataSourceID());
        assertEquals(areaId, platform.getAreaId());
        assertTrue(isMarkedInterchange);

        Service service = MutableService.build(StringIdFor.createId("serviceId"));
        TramTime dropOffTime = TramTime.of(8,15);
        TramTime pickupTime = TramTime.of(8, 20);

        assertTrue(platform.getDropoffRoutes().isEmpty());
        final Route tramTestRoute = TestEnv.getTramTestRoute();
        platform.addRouteDropOff(tramTestRoute, service, dropOffTime);
        assertEquals(1, platform.getDropoffRoutes().size());
        assertTrue(platform.getDropoffRoutes().contains(tramTestRoute));

        assertEquals(1, platform.getDropoffRoutes().size());

        Route anotherRoute = TestEnv.getTramTestRoute(Route.createId("anotherRoute"), "routeNameB");

        platform.addRoutePickUp(anotherRoute, service, pickupTime);
        assertEquals(1, platform.getDropoffRoutes().size());
        assertTrue(platform.getPickupRoutes().contains(anotherRoute));

        final Set<Route> routes = platform.getPickupRoutes();
        assertEquals(1, routes.size());
        assertTrue(routes.contains(anotherRoute));

        final Set<TransportMode> transportModes = platform.getTransportModes();
        assertEquals(1, transportModes.size());
        assertTrue(transportModes.contains(tramTestRoute.getTransportMode()));

    }


}
