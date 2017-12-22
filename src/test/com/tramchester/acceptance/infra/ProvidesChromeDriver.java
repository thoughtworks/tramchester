package com.tramchester.acceptance.infra;

import com.tramchester.domain.presentation.LatLong;
import org.json.simple.JSONObject;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.html5.Location;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.nio.file.Path;

public class ProvidesChromeDriver extends ProvidesDesktopDriver {
    private final DesiredCapabilities capabilities;
    private final ChromeOptions chromeOptions;
    private LatLong location;

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
    public void setStubbedLocation(LatLong location) {
        this.location = location;
    }
}
