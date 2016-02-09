package com.tramchester.domain.presentation;

import com.tramchester.domain.Station;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DisplayStationTest {

    @Test
    public void shouldGetCorrectProximityGroup() {
        Station station = new Station("id", "area", "stopName", 0.1D, -2D, true);
        DisplayStation displayStation = new DisplayStation(station, "ProxGroup");
        assertEquals("ProxGroup", displayStation.getProximityGroup());
    }
}
