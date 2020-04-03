package com.tramchester.unit.environment;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.lang.String.format;
import static junit.framework.TestCase.assertTrue;

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
        String rawFilename = System.getenv(envVarName);
        if (rawFilename!=null) {
            Path path = Paths.get(rawFilename);
            assertTrue(format("Checkfailed for %s value '%s' path '%s'", envVarName, rawFilename, path),
                    Files.exists(path));
        }
    }

}
