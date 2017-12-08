package com.tramchester.acceptance.infra;

import com.tramchester.domain.presentation.LatLong;
import org.apache.commons.io.FileUtils;
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

    public ProvidesFirefoxDriver(boolean enableGeo) {
        capabilities = createCapabilities(enableGeo);
    }

    @Override
    public void init() {
        String firefoxPath = System.getenv("FIREFOX_PATH");
        if (firefoxPath!=null) {
            System.setProperty("webdriver.firefox.bin", firefoxPath);
        }
        driver = new FirefoxDriver(capabilities);
        driver.manage().deleteAllCookies();
    }

    @Override
    public void setStubbedLocation(LatLong location) throws IOException {
        createGeoFile(location);
        FirefoxProfile profile = new FirefoxProfile();
        profile.setPreference("geo.prompt.testing", true);
        profile.setPreference("geo.prompt.testing.allow", true);
        profile.setPreference("geo.wifi.uri", "file://" + locationStubJSON.toAbsolutePath().toString());
        capabilities.setCapability(FirefoxDriver.PROFILE, profile);
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
