package com.tramchester.domain.presentation;

import com.tramchester.domain.Station;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DisplayStationTest {

    @Test
    public void shouldGetCorrectProximityGroup() {
        Station station = new Station("id", "area", "stopName", new LatLong(0.1D, -2D), true);
        DisplayStation displayStation = new DisplayStation(station, new ProximityGroup(5,"ProxGroup"));
        assertEquals(new ProximityGroup(5,"ProxGroup"), displayStation.getProximityGroup());
    }
}
