package com.tramchester.acceptance.infra;

import com.tramchester.acceptance.pages.RoutePlannerPage;
import com.tramchester.acceptance.pages.WelcomePage;
import com.tramchester.domain.presentation.LatLong;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.remote.MobileCapabilityType;
import org.junit.rules.TestName;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.UnexpectedAlertBehaviour;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.nio.file.Path;

public class ProvidesChromeOnAndroid extends ProvidesDriver {

    private final boolean enableGeo;
    private AppiumDriver driver;

    public ProvidesChromeOnAndroid(boolean enableGeo) {
        this.enableGeo = enableGeo;
        // TODO - the non-geo tests won't pass until this is done
    }

    @Override
    public void init() {
        DesiredCapabilities capabilities = new DesiredCapabilities();
        capabilities.setCapability(MobileCapabilityType.PLATFORM_NAME, "Android");
        //capabilities.setCapability(MobileCapabilityType.PLATFORM_VERSION, "4.4");
        capabilities.setCapability(MobileCapabilityType.DEVICE_NAME, "Pixel_API_25");
        capabilities.setCapability(MobileCapabilityType.BROWSER_NAME, "Chrome");
        capabilities.setCapability(MobileCapabilityType.NEW_COMMAND_TIMEOUT, 10);
        capabilities.setCapability(MobileCapabilityType.TAKES_SCREENSHOT, "true");
        capabilities.setCapability(MobileCapabilityType.UNEXPECTED_ALERT_BEHAVIOUR, UnexpectedAlertBehaviour.DISMISS);

        driver = new AndroidDriver(capabilities);
    }

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

    @Override
    protected TakesScreenshot getDriver() {
        return driver;
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

    @Override
    public void setStubbedLocation(LatLong location) {
        // TODO
    }
}
