package com.tramchester.unit.domain;


import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TestStation;
import org.junit.jupiter.api.Test;

import static com.tramchester.domain.Agency.Walking;
import static com.tramchester.domain.reference.TransportMode.Train;
import static com.tramchester.domain.reference.TransportMode.Tram;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
                new LatLong(-2.0, 2.3), TransportMode.Bus, DataSourceID.tfgm);

        assertEquals("stopName", busStation.getName());
        assertEquals(StringIdFor.createId("id"), busStation.getId());
        assertEquals(-2.0, busStation.getLatLong().getLat(),0);
        assertEquals(2.3, busStation.getLatLong().getLon(),0);
        assertEquals("area", busStation.getArea());
        //assertTrue(TransportMode.isBus(busStation));
    }

    @Test
    void shouldHaveCorrectTransportModes() {
        Station station = new Station(StringIdFor.createId("stationId"), "area", "name", TestEnv.nearPiccGardens,
                CoordinateTransforms.getGridPosition(TestEnv.nearPiccGardens), DataSourceID.tfgm);

        assertTrue(station.getTransportModes().isEmpty());

        station.addRoute(new Route(StringIdFor.createId("routeIdA"), "shortName", "name", TestEnv.MetAgency(), Tram));
        assertTrue(station.serves(Tram));

        station.addRoute(new Route(StringIdFor.createId("routeIdB"), "trainShort", "train", Walking, Train));
        assertTrue(station.serves(Train));

        assertEquals(2, station.getTransportModes().size());
    }


}
