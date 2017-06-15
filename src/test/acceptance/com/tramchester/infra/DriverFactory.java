package com.tramchester.infra;

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
}
