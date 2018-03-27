package com.tramchester.acceptance.infra;

import com.tramchester.acceptance.pages.ProvidesDateInput;
import com.tramchester.acceptance.pages.RoutePlannerPage;
import com.tramchester.acceptance.pages.WelcomePage;
import com.tramchester.domain.presentation.LatLong;
import org.apache.commons.io.FileUtils;
import org.junit.rules.TestName;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class ProvidesFirefoxDriver extends ProvidesDesktopDriver {
    private Path locationStubJSON = Paths.get("geofile.json");

    private final DesiredCapabilities capabilities;
    private ProvidesDateInput providesDateInput;

    public ProvidesFirefoxDriver(boolean enableGeo) {
        capabilities = createCapabilities(enableGeo);
    }

    @Override
    public void commonAfter(TestName testName) {
        try {
            if (driver!=null) {
                takeScreenShot(testName);
                // not supported with geckodriver, sigh
//                LogEntries logs = driver.manage().logs().get(LogType.BROWSER);
//                logs.forEach(log -> System.out.println(log));
            }
        } finally {
            if (driver!=null) {
                driver.close();
            }
        }
    }

    @Override
    public void init() {
        providesDateInput = new ProvidesFirefoxDateInput();
        String firefoxPath = System.getenv("FIREFOX_PATH");
        if (firefoxPath!=null) {
            System.setProperty("webdriver.firefox.bin", firefoxPath);
        }
        String geckoDriver = System.getenv("GECKODRIVER_PATH");
        if (geckoDriver!=null) {
            System.setProperty("webdriver.gecko.driver", geckoDriver);
        }

        FirefoxProfile geoDisabled = new FirefoxProfile();
        geoDisabled.setPreference("geo.enabled", false);
        geoDisabled.setPreference("geo.provider.use_corelocation", false);
        geoDisabled.setPreference("geo.prompt.testing", false);
        geoDisabled.setPreference("geo.prompt.testing.allow", false);
        capabilities.setCapability(FirefoxDriver.PROFILE, geoDisabled);

        capabilities.setCapability("log","trace");

        driver = new FirefoxDriver(capabilities);
        driver.manage().deleteAllCookies();
    }

    @Override
    public RoutePlannerPage getRoutePlannerPage() {
        return new RoutePlannerPage(driver, providesDateInput);
    }

    @Override
    public WelcomePage getWelcomePage() {
        return new WelcomePage(driver, providesDateInput);
    }


    @Override
    public void setStubbedLocation(LatLong location) throws IOException {
        createGeoFile(location);

        //FirefoxProfile profile = new FirefoxProfile();
        //profile.setPreference("geo.prompt.testing", true);
        //profile.setPreference("geo.prompt.testing.allow", true);
        //profile.setPreference("geo.wifi.uri", "file://" + locationStubJSON.toAbsolutePath().toString());
        capabilities.setBrowserName("firefox");

        //capabilities.setCapability(FirefoxDriver.PROFILE, profile);
    }

    private void createGeoFile(LatLong place) throws IOException {
        Files.deleteIfExists(locationStubJSON);

        String json = "{\n" +
                "    \"status\": \"OK\",\n" +
                "    \"accuracy\": 10.0,\n" +
                "    \"location\": {\n" +
                "        \"lat\": " +place.getLat() + ",\n" +
                "        \"lng\": " +place.getLon()+"\n" +
                "     }\n" +
                "}";

        try {
            FileUtils.writeStringToFile(locationStubJSON.toFile(), json, Charset.defaultCharset());
        } catch (IOException e) {
            // this is asserted later
        }
    }
}
