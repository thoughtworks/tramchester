package com.tramchester.unit.domain;


import com.tramchester.domain.IdFor;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.testSupport.TestStation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opengis.referencing.operation.TransformException;

class StationTest {

    @Test
    void testShouldSetTramNameCorrecly() throws TransformException {
        Station tramStation = TestStation.forTest("id", "area", "stopName",
                new LatLong(-2.0, 2.3), TransportMode.Tram);

        Assertions.assertEquals("stopName", tramStation.getName());
        Assertions.assertEquals(IdFor.createId("id"), tramStation.getId());
        Assertions.assertEquals(-2.0, tramStation.getLatLong().getLat(),0);
        Assertions.assertEquals(2.3, tramStation.getLatLong().getLon(),0);
        Assertions.assertEquals("area", tramStation.getArea());
        Assertions.assertTrue(TransportMode.isTram(tramStation));
    }

    @Test
    void testShouldSetBusNameCorrecly() throws TransformException {
        Station busStation = TestStation.forTest("id", "area", "stopName",
                new LatLong(-2.0, 2.3), TransportMode.Bus);

        Assertions.assertEquals("stopName", busStation.getName());
        Assertions.assertEquals(IdFor.createId("id"), busStation.getId());
        Assertions.assertEquals(-2.0, busStation.getLatLong().getLat(),0);
        Assertions.assertEquals(2.3, busStation.getLatLong().getLon(),0);
        Assertions.assertEquals("area", busStation.getArea());
        Assertions.assertTrue(TransportMode.isBus(busStation));
    }

    @Test
    void testShouldFormIdByRemovingPlatformForTramStop() {
        Assertions.assertEquals(IdFor.createId("9400ZZid"), Station.formId("9400ZZid1"));
        Assertions.assertEquals(IdFor.createId("9400XXid1"), Station.formId("9400XXid1"));

    }


}
