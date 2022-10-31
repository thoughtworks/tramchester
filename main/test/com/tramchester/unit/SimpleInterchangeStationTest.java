package com.tramchester.unit;

import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.*;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.Test;

import static com.tramchester.testSupport.reference.KnownLocations.nearStPetersSquare;
import static org.junit.jupiter.api.Assertions.*;

public class SimpleInterchangeStationTest {

    @Test
    void shouldHaveCreateSingleModeInterchange() {

        Route routeOne = TestEnv.getTramTestRoute(Route.createId("route1"), "route 1 name");
        Route routeTwo = TestEnv.getTramTestRoute(Route.createId("route2"), "route 2 name");

        IdFor<Station> stationId = Station.createId("stationId");
        MutableStation station = new MutableStation(stationId, NaptanArea.createId("naptanId"),
                "station name", nearStPetersSquare.latLong(), nearStPetersSquare.grid(),  DataSourceID.tfgm);
        station.addRouteDropOff(routeOne);
        station.addRoutePickUp(routeTwo);

        SimpleInterchangeStation interchangeStation = new SimpleInterchangeStation(station, InterchangeType.FromConfig);

        assertFalse(interchangeStation.isMultiMode());

        assertEquals(stationId, interchangeStation.getStationId());
        assertEquals(station, interchangeStation.getStation());

        assertEquals(1, interchangeStation.getPickupRoutes().size());
        assertTrue(interchangeStation.getPickupRoutes().contains(routeTwo));

        assertEquals(1, interchangeStation.getDropoffRoutes().size());
        assertTrue(interchangeStation.getDropoffRoutes().contains(routeOne));

    }
}
