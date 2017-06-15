package com.tramchester.infra;

import com.tramchester.pages.RoutePlannerPage;
import com.tramchester.pages.WelcomePage;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.remote.MobileCapabilityType;
import org.junit.rules.TestName;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.remote.DesiredCapabilities;

public class ProvidesAppiumDriver implements ProvidesDriver {

    private AppiumDriver driver;

    @Override
    public void init() {
        DesiredCapabilities capabilities = new DesiredCapabilities();
        capabilities.setCapability(MobileCapabilityType.PLATFORM_NAME, "Android");
        //capabilities.setCapability(MobileCapabilityType.PLATFORM_VERSION, "4.4");
        capabilities.setCapability(MobileCapabilityType.DEVICE_NAME, "Nexus_5X_API_24");
        capabilities.setCapability(MobileCapabilityType.BROWSER_NAME, "Chrome");
        driver = new AndroidDriver(capabilities);
    }

    @Override
    public void commonAfter(TestName testName) {
        if (driver!=null) {
            driver.close();
        }
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
