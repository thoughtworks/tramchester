package com.tramchester.unit.domain.places;

import com.tramchester.domain.places.PostcodeLocation;
import com.tramchester.domain.presentation.LatLong;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PostcodeLocationTest {

    @Test
    void shouldGetAreaAndName() {
        LatLong latLon = new LatLong(1,1);
        PostcodeLocation locationA = new PostcodeLocation(latLon, PostcodeLocation.createId("M17AB"));
        Assertions.assertEquals("M17AB", locationA.getName());
        Assertions.assertEquals("M17AB", locationA.forDTO());

        PostcodeLocation locationB = new PostcodeLocation(latLon, PostcodeLocation.createId("wa114ab"));
        Assertions.assertEquals("WA114AB", locationB.getName());
        Assertions.assertEquals("WA114AB", locationB.forDTO());

        PostcodeLocation locationC = new PostcodeLocation(latLon, PostcodeLocation.createId("B114AB"));
        Assertions.assertEquals("B114AB", locationC.getName());
        Assertions.assertEquals("B114AB", locationC.forDTO());

    }
}
