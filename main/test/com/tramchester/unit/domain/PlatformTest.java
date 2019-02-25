package com.tramchester.unit.domain;

import com.tramchester.domain.Platform;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PlatformTest {

    @Test
    public void shouldCreatePlatformCorrectly() {
        Platform platform = new Platform("9400ZZ_Name2", "StationName");

        assertEquals("StationName platform 2", platform.getName());
        assertEquals("9400ZZ_Name2", platform.getId());
        assertEquals( "2", platform.getPlatformNumber());
    }


}
