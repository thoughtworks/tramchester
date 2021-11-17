package com.tramchester.unit.domain;

import com.tramchester.domain.Route;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.MutablePlatform;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlatformTest {

    @Test
    void shouldCreatePlatformCorrectly() {
        MutablePlatform platform = new MutablePlatform("9400ZZ_Name2", "StationName", TestEnv.nearAltrincham);

        assertEquals("StationName platform 2", platform.getName());
        assertEquals(StringIdFor.createId("9400ZZ_Name2"), platform.getId());
        assertEquals( "2", platform.getPlatformNumber());
        assertEquals(TestEnv.nearAltrincham, platform.getLatLong());

        assertTrue(platform.getRoutes().isEmpty());
        final Route tramTestRoute = TestEnv.getTramTestRoute();
        platform.addRoute(tramTestRoute);

        assertEquals(1, platform.getRoutes().size());
        assertEquals(tramTestRoute, platform.getRoutes().iterator().next());

    }


}
