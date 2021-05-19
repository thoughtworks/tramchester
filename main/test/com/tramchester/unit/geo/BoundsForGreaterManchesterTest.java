package com.tramchester.unit.geo;

import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.BoundingBox;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class BoundsForGreaterManchesterTest {

    @Test
    void testShouldHaveCorrectBoundsForGreaterManchester() {

        // see
        // https://en.wikipedia.org/wiki/Geography_of_Greater_Manchester
        // https://tfgm.com/public-transport/tram/geographical/network-map
        LatLong northernmost = new LatLong(53.685831, -2.133964);
        LatLong southernmost = new LatLong(53.327158, -2.161361);
        LatLong westernmost = new LatLong(53.520083, -2.730694);
        LatLong easternmost = new LatLong(53.538464, -1.910183);

        BoundingBox testBounds = TestEnv.getTFGMBusBounds();

        assertTrue(testBounds.contained(northernmost));
        assertTrue(testBounds.contained(southernmost));
        assertTrue(testBounds.contained(westernmost));
        assertTrue(testBounds.contained(easternmost));

    }
}
