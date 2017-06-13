package com.tramchester;

import com.tramchester.pages.JourneyDetailsPage;
import com.tramchester.pages.RouteDetailsPage;
import com.tramchester.pages.RoutePlannerPage;
import com.tramchester.pages.WelcomePage;
import org.joda.time.LocalDate;
import org.junit.rules.TestName;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.UnexpectedAlertBehaviour;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringEndsWith.endsWith;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertTrue;


public class UserJourneys {
    protected FirefoxDriver driver;
    protected static final String configPath = "config/localAcceptance.yml";
    protected int expectedNumberJourneyResults = 3; // depends on frequency and timewindow

    protected void takeScreenShot(TestName testName) {
        if (driver==null) {
            return;
        }
        try {
            TakesScreenshot takesScreenshot = (TakesScreenshot) driver;
            byte[] bytes = takesScreenshot.getScreenshotAs(OutputType.BYTES);
            File target = new File(format("build/reports/tests/%s.png", testName.getMethodName()));
            FileOutputStream output = new FileOutputStream(target);
            output.write(bytes);
            output.close();
        } catch (IOException e) {
            // unable to take screenshot
        }
    }

    protected void commonAfter(TestName testName) {
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

    protected DesiredCapabilities createCommonCapabilities(boolean enableGeo) {
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

}
