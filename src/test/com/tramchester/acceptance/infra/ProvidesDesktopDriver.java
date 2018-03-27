package com.tramchester.acceptance.infra;


import com.tramchester.acceptance.pages.WelcomePage;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.UnexpectedAlertBehaviour;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.util.logging.Level;

public abstract class ProvidesDesktopDriver extends ProvidesDriver {

    protected WebDriver driver;

    protected DesiredCapabilities createCapabilities(boolean enableGeo) {
        DesiredCapabilities caps = new DesiredCapabilities();

        LoggingPreferences loggingPrefs = new LoggingPreferences();
        loggingPrefs.enable(LogType.BROWSER, Level.FINE);
        caps.setCapability(CapabilityType.LOGGING_PREFS, loggingPrefs);

        // not working with geckodriver
        caps.setCapability(CapabilityType.SUPPORTS_LOCATION_CONTEXT, enableGeo);
        // ACCEPT OR DISMISS?
        caps.setCapability(CapabilityType.UNEXPECTED_ALERT_BEHAVIOUR, UnexpectedAlertBehaviour.DISMISS);
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
