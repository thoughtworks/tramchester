package com.tramchester.acceptance.infra;

import com.tramchester.acceptance.pages.ProvidesDateInput;
import com.tramchester.acceptance.pages.RoutePlannerPage;
import com.tramchester.acceptance.pages.WelcomePage;
import com.tramchester.domain.presentation.LatLong;
import org.junit.rules.TestName;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.html5.Location;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.remote.DesiredCapabilities;

public class ProvidesChromeDriver extends ProvidesDesktopDriver {
    private final DesiredCapabilities capabilities;
    private final ChromeOptions chromeOptions;
    private LatLong location;
    private ProvidesDateInput providesDateInput;

    public ProvidesChromeDriver(boolean enableGeo) {
        String chromedriverPath = System.getenv("CHROMEDRIVER_PATH");
        if (chromedriverPath!=null) {
            System.setProperty("webdriver.chrome.driver",chromedriverPath);
        }

        capabilities = createCapabilities(enableGeo);

        chromeOptions = new ChromeOptions();
        if (!enableGeo) {
            // geolocation in headless doesn't seem to work
            chromeOptions.addArguments("--headless");
        }

        if (!enableGeo) {
            chromeOptions.addArguments("--disable-geolocation");
        } else {
            chromeOptions.addArguments("--enable-geolocation");
        }

        providesDateInput = new ProvidesChromeDateInput();
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
    public void init() {

        capabilities.setCapability(ChromeOptions.CAPABILITY, chromeOptions);
        ChromeDriver chromeDriver = new ChromeDriver(capabilities);
        driver = chromeDriver;
        driver.manage().deleteAllCookies();

        if (location!=null) {
            chromeDriver.setLocation(new Location(location.getLat(),location.getLon(),0));
        }
    }

    @Override
    public RoutePlannerPage getRoutePlannerPage() {
        return new RoutePlannerPage(driver,providesDateInput);
    }

    @Override
    public WelcomePage getWelcomePage() {
        return new WelcomePage(driver, providesDateInput);
    }

    @Override
    public void setStubbedLocation(LatLong location) {
        this.location = location;
    }
}
