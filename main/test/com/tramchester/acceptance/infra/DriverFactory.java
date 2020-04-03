package com.tramchester.acceptance.infra;

import java.util.HashMap;
import java.util.Map;

public class DriverFactory {

    private Map<String, ProvidesDriver> drivers;

    public DriverFactory() {
        drivers = new HashMap<>();
    }

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
            case "firefox":
                return new ProvidesFirefoxDriver(enableGeo);
            case "chrome":
                return new ProvidesChromeDriver(enableGeo);
            case "androidChrome":
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
}
