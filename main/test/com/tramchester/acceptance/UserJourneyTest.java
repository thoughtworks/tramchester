package com.tramchester.acceptance;

import com.tramchester.acceptance.infra.DriverFactory;
import com.tramchester.acceptance.infra.ProvidesChromeDriver;
import com.tramchester.acceptance.infra.ProvidesDriver;
import com.tramchester.acceptance.infra.ProvidesFirefoxDriver;
import com.tramchester.acceptance.pages.App.AppPage;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.TestInfo;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class UserJourneyTest {
    private static DriverFactory driverFactory;

    protected static void createFactory(boolean enableGeoLocation) {
        if (driverFactory!=null) {
            throw new RuntimeException("Factory already created");
        }
        driverFactory = new DriverFactory(enableGeoLocation);
    }

    protected static void closeFactory() {
        driverFactory.close();
        driverFactory.quit();
        driverFactory = null;
    }

    protected AppPage prepare(ProvidesDriver providesDriver, String url) {
        providesDriver.init();
        providesDriver.clearCookies();

        AppPage appPage = providesDriver.getAppPage();
        appPage.load(url);

        assertTrue(appPage.waitForCookieAgreementVisible(), "cookie agreement visible");
        appPage.agreeToCookies();
        assertTrue(appPage.waitForCookieAgreementInvisible(), "wait for cookie agreement to close");
        assertTrue(appPage.waitForReady(), "app ready");
        assertTrue(appPage.waitForToStops(), "stops appeared");

        return appPage;
    }

    public static Stream<ProvidesDriver> getProviderCommon() {

        List<String> names;
        if (!TestEnv.isCircleci()) {
            names = Arrays.asList("chrome", "firefox");
        } else {
            // TODO - confirm this is still an issue
            // Headless Chrome on CI BOX is ignoring locale which breaks many acceptance tests
            // https://bugs.chromium.org/p/chromium/issues/detail?id=755338
            names = Collections.singletonList("firefox");
        }
        return names.stream().map(browserName -> driverFactory.get(browserName));
    }

    protected void takeScreenshotsFor(TestInfo testInfo) {
        String displayName = testInfo.getDisplayName();

        if (displayName.contains(ProvidesFirefoxDriver.Name)) {
            driverFactory.takeScreenshotFor(ProvidesFirefoxDriver.Name, displayName);
        } else if (displayName.contains(ProvidesChromeDriver.Name)) {
            driverFactory.takeScreenshotFor(ProvidesChromeDriver.Name, displayName);
        }
    }

}
