package com.tramchester.acceptance.infra;

import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.nio.file.Path;

public class ProvidesChromeDriver extends ProvidesDesktopDriver {
    private final DesiredCapabilities capabilities;

    public ProvidesChromeDriver(boolean enableGeo) {
        System.setProperty("webdriver.chrome.driver","lib/chromedriver2.30");
        capabilities = createCapabilities(enableGeo);
    }

    @Override
    public void init() {
        driver = new ChromeDriver(capabilities);
        driver.manage().deleteAllCookies();
    }

    @Override
    public void setProfileForGeoFile(Path fullPath) {
        // TODO
    }
}
