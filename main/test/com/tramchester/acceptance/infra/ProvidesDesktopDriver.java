package com.tramchester.acceptance.infra;


import org.junit.rules.TestName;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.util.logging.Level;

public abstract class ProvidesDesktopDriver extends ProvidesDriver {

    protected WebDriver driver = null;

    protected DesiredCapabilities createCapabilities() {
        DesiredCapabilities caps = new DesiredCapabilities();

        LoggingPreferences loggingPrefs = new LoggingPreferences();

        loggingPrefs.enable(LogType.BROWSER, Level.SEVERE);
        loggingPrefs.enable(LogType.DRIVER, Level.SEVERE);

        caps.setCapability(CapabilityType.LOGGING_PREFS, loggingPrefs);

        return caps;
    }

    @Override
    public void commonAfter(TestName testName) {
        if (driver!=null) {
            takeScreenShot(testName);
        }
    }

    @Override
    protected RemoteWebDriver getDriver() {
        return (RemoteWebDriver) driver;
    }

    @Override
    public Cookie getCookieNamed(String name) {
        return driver.manage().getCookieNamed(name);
    }

}
