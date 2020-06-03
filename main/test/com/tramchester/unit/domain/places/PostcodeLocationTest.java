package com.tramchester.unit.domain.places;

import com.tramchester.domain.places.PostcodeLocation;
import com.tramchester.domain.presentation.LatLong;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PostcodeLocationTest {

    @Test
    public void shouldGetAreaAndName() {
        LatLong latLon = new LatLong(1,1);
        PostcodeLocation locationA = new PostcodeLocation(latLon, "M17AB");
        assertEquals("M1", locationA.getArea());
        assertEquals("M17AB", locationA.getName());
        assertEquals("M17AB", locationA.getId());
        PostcodeLocation locationB = new PostcodeLocation(latLon, "WA114AB");
        assertEquals("WA11", locationB.getArea());
        assertEquals("WA114AB", locationB.getName());
        assertEquals("WA114AB", locationB.getId());
        PostcodeLocation locationC = new PostcodeLocation(latLon, "B114AB");
        assertEquals("B11", locationC.getArea());
        assertEquals("B114AB", locationC.getName());
        assertEquals("B114AB", locationC.getId());

    }
}
