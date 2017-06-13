package com.tramchester;

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
import java.util.logging.Level;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;


public class ProvidesDriver {
    private final DesiredCapabilities capabilities;
    protected FirefoxDriver driver;

    public ProvidesDriver(boolean enableGeo) {
        capabilities = createCommonCapabilities(enableGeo);
    }

    public void init() {
        driver = new FirefoxDriver(capabilities);
        driver.manage().deleteAllCookies();
    }

    private void takeScreenShot(TestName testName) {
        if (driver==null) {
            return;
        }
        try {
            TakesScreenshot takesScreenshot = driver;
            byte[] bytes = takesScreenshot.getScreenshotAs(OutputType.BYTES);
            File target = new File(format("build/reports/tests/%s.png", testName.getMethodName()));
            FileOutputStream output = new FileOutputStream(target);
            output.write(bytes);
            output.close();
        } catch (IOException e) {
            // unable to take screenshot
        }
    }

    public void commonAfter(TestName testName) {
        try {
            takeScreenShot(testName);
            LogEntries logs = driver.manage().logs().get(LogType.BROWSER);
            logs.forEach(log -> System.out.println(log));
        } finally {
            if (driver!=null) {
                driver.close();
            }
        }
    }

    public DesiredCapabilities createCommonCapabilities(boolean enableGeo) {
        String firefoxPath = System.getenv("FIREFOX_PATH");
        if (firefoxPath!=null) {
            System.setProperty("webdriver.firefox.bin", firefoxPath);
        }

        DesiredCapabilities capabilities = new DesiredCapabilities();
        capabilities.setCapability(CapabilityType.SUPPORTS_LOCATION_CONTEXT, enableGeo);
        capabilities.setCapability(CapabilityType.UNEXPECTED_ALERT_BEHAVIOUR, UnexpectedAlertBehaviour.ACCEPT);
        LoggingPreferences loggingPrefs = new LoggingPreferences();
        loggingPrefs.enable(LogType.BROWSER, Level.FINE);
        capabilities.setCapability(CapabilityType.LOGGING_PREFS, loggingPrefs);
        return capabilities;
    }

    public void setProfile(FirefoxProfile profile) {
        capabilities.setCapability(FirefoxDriver.PROFILE, profile);
    }

    public WelcomePage getWelcomePage() {
        return new WelcomePage(driver);
    }

    public Cookie getCookieNamed(String name) {
        return driver.manage().getCookieNamed(name);
    }

    public RoutePlannerPage getRoutePlannerPage() throws InterruptedException {
        return new RoutePlannerPage(driver);
    }
}
