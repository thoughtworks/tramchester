package com.tramchester.unit.domain;

import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.Platform;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlatformTest {

    @Test
    void shouldCreatePlatformCorrectly() {
        Platform platform = new Platform("9400ZZ_Name2", "StationName", TestEnv.nearAltrincham);

        assertEquals("StationName platform 2", platform.getName());
        assertEquals(StringIdFor.createId("9400ZZ_Name2"), platform.getId());
        assertEquals( "2", platform.getPlatformNumber());
        assertEquals(TestEnv.nearAltrincham, platform.getLatLong());
    }


}
