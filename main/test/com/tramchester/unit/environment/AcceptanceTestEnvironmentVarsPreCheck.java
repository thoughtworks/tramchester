package com.tramchester.unit.environment;

import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

// want to find out as soon as possible, not wait for integration tests
class AcceptanceTestEnvironmentVarsPreCheck {

    @Test
    void firefoxBinaryPathShouldBeValidIfSet() {
        assertPathValid("FIREFOX_PATH");
    }

    @Test
    void chromedriverPathShouldBeValidIfSet() {
        assertPathValid("CHROMEDRIVER_PATH");
    }

    @Test
    void geckodriverPathShouldBeValidIfSet() {
        assertPathValid("GECKODRIVER_PATH");
    }

    private void assertPathValid(String envVarName) {
        Path path = TestEnv.getPathFromEnv(envVarName);
        if (path != null) {
            Assertions.assertTrue(Files.exists(path), "Check failed for "+ envVarName);
        }
    }

}
