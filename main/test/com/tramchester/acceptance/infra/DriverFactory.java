package com.tramchester.acceptance.infra;

public class DriverFactory {

    public static ProvidesDriver create(boolean enableGeo, String browserName) {
        if (browserName.equals("firefox")) {
            return new ProvidesFirefoxDriver(enableGeo);
        } else if (browserName.equals("chrome")) {
            return new ProvidesChromeDriver(enableGeo);
        } else if (browserName.equals("androidChrome")) {
            return new ProvidesChromeOnAndroid(enableGeo);
        } else {
            throw new RuntimeException("Unknown browser "+browserName);
        }
    }
}
