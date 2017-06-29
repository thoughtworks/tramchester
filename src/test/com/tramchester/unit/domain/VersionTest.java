package com.tramchester.unit.domain;


import com.tramchester.domain.Version;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class VersionTest {

    @Test
    public void shouldReportCorrectVersion() {
        Version version = new Version("someNumber");
        assertEquals("someNumber", version.getBuildNumber());
    }
}
