package com.tramchester.unit.domain;


import com.tramchester.domain.presentation.Version;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

class VersionTest {

    @Test
    void shouldReportCorrectVersion() {
        Version version = new Version("versionString");
        Assertions.assertEquals("versionString", version.getBuildNumber());
    }
}
