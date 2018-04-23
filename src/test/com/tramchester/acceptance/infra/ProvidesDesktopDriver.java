package com.tramchester.acceptance.infra;


import org.openqa.selenium.Cookie;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.util.logging.Level;

public abstract class ProvidesDesktopDriver extends ProvidesDriver {

    protected WebDriver driver;

    protected DesiredCapabilities createCapabilities() {
        DesiredCapabilities caps = new DesiredCapabilities();

        LoggingPreferences loggingPrefs = new LoggingPreferences();
        loggingPrefs.enable(LogType.BROWSER, Level.ALL);

        loggingPrefs.enable(LogType.DRIVER, Level.ALL);

        caps.setCapability(CapabilityType.LOGGING_PREFS, loggingPrefs);

        return caps;
    }

    @Override
    protected TakesScreenshot getDriver() {
        return (TakesScreenshot) driver;
    }

    @Override
    public Cookie getCookieNamed(String name) {
        return driver.manage().getCookieNamed(name);
    }

}
