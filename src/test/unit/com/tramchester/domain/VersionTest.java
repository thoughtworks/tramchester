package com.tramchester.domain;


import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class VersionTest {

    @Test
    public void shouldReportCorrectVersion() {
        Version version = new Version("someNumber");
        assertEquals("someNumber", version.getBuildNumber());
    }
}
