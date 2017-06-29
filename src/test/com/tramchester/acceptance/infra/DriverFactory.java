package com.tramchester.acceptance.infra;

import java.util.Optional;

public class DriverFactory {

    public static ProvidesDriver create(boolean enableGeo) {
        Optional<String> appiumFlag = Optional.ofNullable(System.getProperty("appium"));

        if (appiumFlag.isPresent()) {
            if (appiumFlag.get().equals("true")) {
                return new ProvidesAppiumDriver(enableGeo);
            }
        }
        return new ProvidesFirefoxDriver(enableGeo);

    }

    public static ProvidesDriver create(boolean enableGeo, String browserName) {
        if (browserName.equals("firefox")) {
            return new ProvidesFirefoxDriver(enableGeo);
        } else if (browserName.equals("chrome")) {
            return new ProvidesChromeDriver(enableGeo);
        } else {
            throw new RuntimeException("Unknown browser "+browserName);
        }
    }
}
