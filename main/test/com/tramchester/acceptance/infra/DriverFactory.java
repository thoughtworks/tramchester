package com.tramchester.acceptance.infra;

import java.util.HashMap;
import java.util.Map;

public class DriverFactory {

    private final Map<String, ProvidesDriver> drivers;

    // Map Name -> Driver Instance
    public DriverFactory() {
        drivers = new HashMap<>();
    }

    // TODO Push enable geo into Cons
    public ProvidesDriver get(boolean enableGeo, String browserName) {
        if (drivers.containsKey(browserName)) {
            if (enableGeo==drivers.get(browserName).isEnabledGeo()) {
                return drivers.get(browserName);
            }
        }
        ProvidesDriver driver = create(enableGeo, browserName);
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
                return new ProvidesChromeOnAndroid(enableGeo);
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
