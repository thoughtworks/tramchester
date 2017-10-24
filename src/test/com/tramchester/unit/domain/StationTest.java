package com.tramchester.unit.domain;


import com.tramchester.domain.Location;
import com.tramchester.domain.Station;
import com.tramchester.domain.presentation.LatLong;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StationTest {

    @Test
    public void testShouldSetTramNameCorrecly() {
        Location tramStation = new Station("id", "area", "stopName", new LatLong(-2.0, 2.3), true);

        assertEquals("stopName", tramStation.getName());
        assertEquals("id", tramStation.getId());
        assertEquals(-2.0, tramStation.getLatLong().getLat(),0);
        assertEquals(2.3, tramStation.getLatLong().getLon(),0);
        assertEquals("area", tramStation.getArea());
        assertTrue(tramStation.isTram());
    }

    @Test
    public void testShouldSetBusNameCorrecly() {
        Location busStation = new Station("id", "area", "stopName",new LatLong(-2.0, 2.3), false);

        assertEquals("area,stopName", busStation.getName());
        assertEquals("id", busStation.getId());
        assertEquals(-2.0, busStation.getLatLong().getLat(),0);
        assertEquals(2.3, busStation.getLatLong().getLon(),0);
        assertEquals("area", busStation.getArea());
        assertTrue(!busStation.isTram());
    }

    @Test
    public void testShouldFormIdByRemovingPlatformForTramStop() {
        assertEquals("9400ZZid", Station.formId("9400ZZid1"));
        assertEquals("9400XXid1", Station.formId("9400XXid1"));

    }


}
