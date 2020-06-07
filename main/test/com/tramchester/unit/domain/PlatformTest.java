package com.tramchester.unit.domain;

import com.tramchester.domain.Platform;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PlatformTest {

    @Test
    void shouldCreatePlatformCorrectly() {
        Platform platform = new Platform("9400ZZ_Name2", "StationName");

        Assertions.assertEquals("StationName platform 2", platform.getName());
        Assertions.assertEquals("9400ZZ_Name2", platform.getId());
        Assertions.assertEquals( "2", platform.getPlatformNumber());
    }


}
