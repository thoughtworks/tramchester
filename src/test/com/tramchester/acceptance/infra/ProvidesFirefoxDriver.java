package com.tramchester.acceptance.infra;

import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.nio.file.Path;


public class ProvidesFirefoxDriver extends ProvidesDesktopDriver {
    private final DesiredCapabilities capabilities;

    public ProvidesFirefoxDriver(boolean enableGeo) {
        capabilities = createCapabilities(enableGeo);
    }

    @Override
    public void init() {
        String firefoxPath = System.getenv("FIREFOX_PATH");
        if (firefoxPath!=null) {
            System.setProperty("webdriver.firefox.bin", firefoxPath);
        }
        driver = new FirefoxDriver(capabilities);
        driver.manage().deleteAllCookies();
    }

    @Override
    public void setProfileForGeoFile(Path path) {
        FirefoxProfile profile = new FirefoxProfile();
        profile.setPreference("geo.prompt.testing", true);
        profile.setPreference("geo.prompt.testing.allow", true);
        profile.setPreference("geo.wifi.uri", "file://" + path.toAbsolutePath().toString());
        capabilities.setCapability(FirefoxDriver.PROFILE, profile);
    }
}
