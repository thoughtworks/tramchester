package com.tramchester.infra;

import com.tramchester.pages.RoutePlannerPage;
import com.tramchester.pages.WelcomePage;
import org.junit.rules.TestName;
import org.openqa.selenium.*;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;

import static java.lang.String.format;


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
