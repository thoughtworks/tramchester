package com.tramchester.domain;


import org.junit.Test;
import org.neo4j.unsafe.impl.batchimport.stats.Stat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StationTest {

    @Test
    public void testShouldSetTramNameCorrecly() {
        Station tramStation = new Station("id", "area", "stopName", -2.0, 2.3, true);

        assertEquals("stopName", tramStation.getName());
        assertEquals("id", tramStation.getId());
        assertEquals(-2.0, tramStation.getLatitude(),0);
        assertEquals(2.3, tramStation.getLongitude(),0);
        assertTrue(tramStation.isTram());
    }

    @Test
    public void testShouldSetBusNameCorrecly() {
        Station busStation = new Station("id", "area", "stopName", -2.0, 2.3, false);

        assertEquals("area,stopName", busStation.getName());
        assertEquals("id", busStation.getId());
        assertEquals(-2.0, busStation.getLatitude(),0);
        assertEquals(2.3, busStation.getLongitude(),0);
        assertTrue(!busStation.isTram());
    }

    @Test
    public void testShouldFormIdByRemovingPlatformForTramStop() {
        assertEquals("9400ZZid", Station.formId("9400ZZid1"));
        assertEquals("9400XXid1", Station.formId("9400XXid1"));

    }


}
