package com.tramchester.unit.domain;


import com.tramchester.domain.input.TramInterchanges;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.testSupport.Stations;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class StationTest {

    @Test
    void shouldHaveCorrectTestDataForStationInterchanges() {
        Assertions.assertEquals(TramInterchanges.stations().size(), Stations.Interchanges.size());
    }

    @Test
    void testShouldSetTramNameCorrecly() {
        Location tramStation = new Station("id", "area", "stopName", new LatLong(-2.0, 2.3), true);

        Assertions.assertEquals("stopName", tramStation.getName());
        Assertions.assertEquals("id", tramStation.getId());
        Assertions.assertEquals(-2.0, tramStation.getLatLong().getLat(),0);
        Assertions.assertEquals(2.3, tramStation.getLatLong().getLon(),0);
        Assertions.assertEquals("area", tramStation.getArea());
        Assertions.assertTrue(tramStation.isTram());
    }

    @Test
    void testShouldSetBusNameCorrecly() {
        Location busStation = new Station("id", "area", "stopName",new LatLong(-2.0, 2.3), false);

        Assertions.assertEquals("area,stopName", busStation.getName());
        Assertions.assertEquals("id", busStation.getId());
        Assertions.assertEquals(-2.0, busStation.getLatLong().getLat(),0);
        Assertions.assertEquals(2.3, busStation.getLatLong().getLon(),0);
        Assertions.assertEquals("area", busStation.getArea());
        Assertions.assertFalse(busStation.isTram());
    }

    @Test
    void testShouldFormIdByRemovingPlatformForTramStop() {
        Assertions.assertEquals("9400ZZid", Station.formId("9400ZZid1"));
        Assertions.assertEquals("9400XXid1", Station.formId("9400XXid1"));

    }


}
