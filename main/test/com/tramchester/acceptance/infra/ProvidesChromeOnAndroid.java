package com.tramchester.acceptance.infra;

import com.tramchester.acceptance.pages.App.AppPage;
import com.tramchester.acceptance.pages.ProvidesDateInput;
import com.tramchester.acceptance.pages.RoutePlannerPage;
import com.tramchester.acceptance.pages.WelcomePage;
import com.tramchester.domain.presentation.LatLong;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.remote.MobileCapabilityType;
import org.junit.rules.TestName;
import org.openqa.selenium.*;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.io.IOException;

public class ProvidesChromeOnAndroid extends ProvidesDriver {

    private final boolean enableGeo;
    private AppiumDriver driver;
    private ProvidesDateInput providesDateInput;

    public ProvidesChromeOnAndroid(boolean enableGeo) {
        this.enableGeo = enableGeo;
        // TODO - the non-geo tests won't pass until this is done
    }

    @Override
    public void init() {
        if (driver==null) {

            providesDateInput = new ProvidesChromeDateInput();

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
    }

    @Override
    public void quit() {
        if (driver!=null) {
            driver.quit();
            driver=null;
        }
    }

    @Override
    public void commonAfter(TestName testName) {
        if (driver!=null) {
            takeScreenShot(testName);
            LogEntries logs = driver.manage().logs().get(LogType.BROWSER);
            logs.forEach(log -> System.out.println(log));
        }
    }

    @Override
    protected RemoteWebDriver getDriver() {
        return driver;
    }

    @Override
    public boolean isEnabledGeo() {
        return enableGeo;
    }

    @Override
    public void updateStubbedLocation(LatLong newLatLong) throws IOException {
        throw new InvalidArgumentException("Not implemented yet");
    }

    @Override
    public WelcomePage getWelcomePage() {
        return new WelcomePage(driver, providesDateInput);
    }

    @Override
    public AppPage getAppPage() {
        return new AppPage(driver, providesDateInput);
    }

    @Override
    public Cookie getCookieNamed(String name) {
        return driver.manage().getCookieNamed(name);
    }

    @Override
    public RoutePlannerPage getRoutePlannerPage() {
        return new RoutePlannerPage(driver, providesDateInput);
    }

    @Override
    public void setStubbedLocation(LatLong location) {
        throw new InvalidArgumentException("Not implemented yet");
    }
}
