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

import java.util.logging.Level;

import static org.openqa.selenium.chrome.ChromeDriverService.CHROME_DRIVER_EXE_PROPERTY;
import static org.openqa.selenium.chrome.ChromeDriverService.CHROME_DRIVER_VERBOSE_LOG_PROPERTY;

public class ProvidesChromeDriver extends ProvidesDesktopDriver {
    private final DesiredCapabilities capabilities;
    private final ChromeOptions chromeOptions;
    private LatLong location;
    private ProvidesDateInput providesDateInput;

    public ProvidesChromeDriver(boolean enableGeo) {
        String chromedriverPath = System.getenv("CHROMEDRIVER_PATH");
        if (chromedriverPath!=null) {
            System.setProperty(CHROME_DRIVER_EXE_PROPERTY,chromedriverPath);
        }
        System.setProperty(CHROME_DRIVER_VERBOSE_LOG_PROPERTY,"false");

        capabilities = createCapabilities();
        chromeOptions = new ChromeOptions();

        // geolocation fails on headless, bug raised https://bugs.chromium.org/p/chromium/issues/detail?id=834808
        chromeOptions.addArguments("--headless");
        if (enableGeo) {
            chromeOptions.addArguments("--enable-geolocation");
        } else {
            chromeOptions.addArguments("--disable-geolocation");
//            chromeOptions.addArguments("--headless");
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

        chromeOptions.merge(capabilities);

        ChromeDriver chromeDriver = new ChromeDriver(chromeOptions);
        chromeDriver.setLogLevel(Level.SEVERE);

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
