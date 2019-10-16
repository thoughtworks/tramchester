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

    protected WebDriver driver;

    protected DesiredCapabilities createCapabilities() {
        DesiredCapabilities caps = new DesiredCapabilities();

        LoggingPreferences loggingPrefs = new LoggingPreferences();

        loggingPrefs.enable(LogType.BROWSER, Level.WARNING);
        loggingPrefs.enable(LogType.DRIVER, Level.WARNING);

        caps.setCapability(CapabilityType.LOGGING_PREFS, loggingPrefs);

        return caps;
    }

    @Override
    public void commonAfter(TestName testName) {
        try {
            if (driver!=null) {
                takeScreenShot(testName);
            }
        } finally {
            if (driver!=null) {
                driver.close();
            }
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
