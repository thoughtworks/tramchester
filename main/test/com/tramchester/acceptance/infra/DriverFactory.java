package com.tramchester.acceptance.infra;

import java.util.HashMap;
import java.util.Map;

public class DriverFactory {

    private final Map<String, ProvidesDriver> drivers;
    private final boolean enableGeoLocation;

    // Map Name -> Driver Instance
    public DriverFactory(boolean enableGeoLocation) {
        this.enableGeoLocation = enableGeoLocation;
        drivers = new HashMap<>();
    }

    public ProvidesDriver get(String browserName) {
        if (drivers.containsKey(browserName)) {
            return drivers.get(browserName);
        }
        ProvidesDriver driver = create(enableGeoLocation, browserName);
        drivers.put(browserName, driver);
        return driver;
    }

    private ProvidesDriver create(boolean enableGeo, String browserName) {
        switch (browserName) {
            case ProvidesFirefoxDriver.Name:
                return new ProvidesFirefoxDriver(enableGeo);
            case ProvidesChromeDriver.Name:
                return new ProvidesChromeDriver(enableGeo);
            case ProvidesChromeOnAndroid.Name:
                return new ProvidesChromeOnAndroid();
            default:
                throw new RuntimeException("Unknown browser " + browserName);
        }
    }

    public void close() {
        drivers.values().forEach(ProvidesDriver::close);
    }

    public void quit() {
        drivers.values().forEach(ProvidesDriver::quit);
        drivers.clear();
    }

    public void takeScreenshotFor(String name, String testName) {
        drivers.get(name).takeScreenShot(testName);
    }
}
