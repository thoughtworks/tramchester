package com.tramchester.acceptance.infra;

import com.tramchester.acceptance.pages.App.AppPage;
import com.tramchester.acceptance.pages.ProvidesDateInput;
import com.tramchester.domain.presentation.LatLong;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.remote.MobileCapabilityType;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.InvalidArgumentException;
import org.openqa.selenium.UnexpectedAlertBehaviour;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

public class ProvidesChromeOnAndroid extends ProvidesDriver {

    public final static String Name ="androidChrome";
    private final boolean enableGeo;
    private AppiumDriver<WebElement> driver;
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
            capabilities.setCapability(MobileCapabilityType.DEVICE_NAME, "Pixel_API_25");
            capabilities.setCapability(MobileCapabilityType.BROWSER_NAME, "Chrome");
            capabilities.setCapability(MobileCapabilityType.NEW_COMMAND_TIMEOUT, 10);
            capabilities.setCapability(MobileCapabilityType.TAKES_SCREENSHOT, "true");
            capabilities.setCapability(MobileCapabilityType.UNEXPECTED_ALERT_BEHAVIOUR, UnexpectedAlertBehaviour.DISMISS);

            driver = new AndroidDriver<>(capabilities);
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
    protected RemoteWebDriver getDriver() {
        return driver;
    }

    @Override
    public boolean isEnabledGeo() {
        return enableGeo;
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
    public void setStubbedLocation(LatLong location) {
        throw new InvalidArgumentException("Not implemented yet");
    }

    @Override
    protected String getDriverName() {
        return Name;
    }
}
