package com.tramchester.infra;


import com.tramchester.pages.RoutePlannerPage;
import com.tramchester.pages.WelcomePage;
import org.junit.rules.TestName;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.UnexpectedAlertBehaviour;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.util.logging.Level;

public abstract class ProvidesDesktopDriver extends ProvidesDriver {

    protected WebDriver driver;

    @Override
    public void commonAfter(TestName testName) {
        try {
            if (driver!=null) {
                takeScreenShot(testName);
                LogEntries logs = driver.manage().logs().get(LogType.BROWSER);
                logs.forEach(log -> System.out.println(log));
            }
        } finally {
            if (driver!=null) {
                driver.close();
            }
        }
    }

    protected DesiredCapabilities createCapabilities(boolean enableGeo) {
        DesiredCapabilities caps = new DesiredCapabilities();

        LoggingPreferences loggingPrefs = new LoggingPreferences();
        loggingPrefs.enable(LogType.BROWSER, Level.FINE);
        caps.setCapability(CapabilityType.LOGGING_PREFS, loggingPrefs);
        caps.setCapability(CapabilityType.SUPPORTS_LOCATION_CONTEXT, enableGeo);
        // ACCEPT OR DISMISS?
        caps.setCapability(CapabilityType.UNEXPECTED_ALERT_BEHAVIOUR, UnexpectedAlertBehaviour.ACCEPT);
        return caps;
    }

    @Override
    protected TakesScreenshot getDriver() {
        return (TakesScreenshot) driver;
    }

    @Override
    public WelcomePage getWelcomePage() {
        return new WelcomePage(driver);
    }

    @Override
    public Cookie getCookieNamed(String name) {
        return driver.manage().getCookieNamed(name);
    }

    @Override
    public RoutePlannerPage getRoutePlannerPage() throws InterruptedException {
        return new RoutePlannerPage(driver);
    }
}
