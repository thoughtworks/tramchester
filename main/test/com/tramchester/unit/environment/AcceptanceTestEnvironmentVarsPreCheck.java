package com.tramchester.unit.environment;

import com.tramchester.testSupport.TestEnv;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static junit.framework.TestCase.assertTrue;

// want to find out as soon as possible, not wait for integration tests
public class AcceptanceTestEnvironmentVarsPreCheck {

    @Test
    public void firefoxBinaryPathShouldBeValidIfSet() {
        assertPathValid("FIREFOX_PATH");
    }

    @Test
    public void chromedriverPathShouldBeValidIfSet() {
        assertPathValid("CHROMEDRIVER_PATH");
    }

    @Test
    public void geckodriverPathShouldBeValidIfSet() {
        assertPathValid("GECKODRIVER_PATH");
    }

    private void assertPathValid(String envVarName) {
        Path path = TestEnv.getPathFromEnv(envVarName);
        if (path != null) {
            assertTrue("Check failed for "+ envVarName, Files.exists(path));
        }
    }

}
