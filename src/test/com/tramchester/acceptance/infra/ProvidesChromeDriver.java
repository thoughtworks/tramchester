package com.tramchester.acceptance.infra;

import com.tramchester.domain.presentation.LatLong;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.nio.file.Path;

public class ProvidesChromeDriver extends ProvidesDesktopDriver {
    private final DesiredCapabilities capabilities;
    private LatLong location;

    public ProvidesChromeDriver(boolean enableGeo) {
        String chromedriverPath = System.getenv("CHROMEDRIVER_PATH");
        if (chromedriverPath!=null) {
            System.setProperty("webdriver.chrome.driver",chromedriverPath);
        }
        capabilities = createCapabilities(enableGeo);
    }

    @Override
    public void init() {
        ChromeDriver chromeDriver = new ChromeDriver(capabilities);
        driver = chromeDriver;
        driver.manage().deleteAllCookies();

        if (location!=null) {
            chromeDriver.executeScript("window.navigator.geolocation.getCurrentPosition = " +
                    "function(success){" +
                    "var position = {\"coords\" : { " +
                    "\"latitude\": \"555\", " +
                    "\"longitude\": \"999\" }" +
                    "};" +
                    "success(position);}");
        }
    }

    @Override
    public void setStubbedLocation(LatLong location) {
        this.location = location;
    }
}
