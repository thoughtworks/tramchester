package com.tramchester;

import com.tramchester.config.AppConfiguration;
import com.tramchester.domain.TramServiceDate;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static java.util.Arrays.asList;


public abstract class TestConfig extends AppConfiguration {

    public static AppConfiguration GET() {
        return new TestConfig() {
            @Override
            public Path getDataFolder() {
                return null;
            }
        };
    }

    private List<String> closedStations = asList("St Peters Square");

    private Path zipFilePath = Paths.get("data.zip");

    private boolean graphExists() {
        return new File(getGraphName()).exists();
    }

    @Override
    public boolean getRebuildGraph() {
        return !graphExists();
    }

    @Override
    public boolean getPullData() {
        File file = getDataFolder().resolve(zipFilePath).toFile();
        return !file.exists();
    }

    @Override
    public List<String> getClosedStations() {
        return closedStations;
    }

    @Override
    public String getInstanceDataUrl() {
        return "http://localhost:8080";
    }

    @Override
    public String getTramDataUrl() {
        return "http://odata.tfgm.com/opendata/downloads/TfGMgtfs.zip";
    }

    @Override
    public Path getDataPath() {
        return getDataFolder();
    }

    @Override
    public int getTimeWindow() { return 60; }

    @Override
    public Double getNearestStopRangeKM() {
        return 2D;
    }

    @Override
    public int getNumOfNearestStops() {
        return 6;
    }

    @Override
    public double getWalkingMPH() {
        return 3;
    }

    public abstract Path getDataFolder();

    @Override
    public boolean getRedirectHTTP() { return false; }

    @Override
    public String getSecureHost() {
        return "tramchester.com";
    }

    @Override
    public int getMaxWait() {
        return 25;
    }

    @Override
    public int getQueryInterval() { return 6; }

    @Override
    public int getRecentStopsToShow() {
        return 3;
    }

    @Override
    public SwaggerBundleConfiguration getSwaggerBundleConfiguration() {
        SwaggerBundleConfiguration bundleConfiguration = new SwaggerBundleConfiguration();
        bundleConfiguration.setResourcePackage("com.tramchester.resources");
        return bundleConfiguration;
    }

    @Override
    public int getDataExpiryThreadhold() { return 3; }

    @Override
    public String getLiveDataUrl() {
        return "https://api.tfgm.com/odata/Metrolinks";
    }

    @Override
    public String getLiveDataSubscriptionKey() {
        return System.getenv("TFGMAPIKEY");
    }

    @Override
    public String getLiveDataS3Bucket() { return "tramchestertestlivedatabucket"; }

    @Override
    public boolean getRemoveRouteNameSuffix() { return false; }

    public static DateTimeFormatter dateFormatDashes = DateTimeFormatter.ofPattern("YYYY-MM-dd");

    ////// WORK IN PROGRESS
    @Override
    public boolean getEdgePerTrip() {
        return true;
    }

    public static LocalDate nextTuesday(int offsetDays) {
        DayOfWeek dayOfWeek = DayOfWeek.TUESDAY;
        LocalDate date = LocalDate.now().minusDays(offsetDays);
        return getNextDate(dayOfWeek, date);
    }

    public static LocalDate getNextDate(DayOfWeek dayOfWeek, LocalDate date) {
        while (date.getDayOfWeek()!= dayOfWeek) {
            date = date.plusDays(1);
        }
        while (new TramServiceDate(date).isChristmasPeriod()) {
            date = date.plusWeeks(1);
        }
        return date;
    }

    public static LocalDate nextSaturday() {
        return getNextDate(DayOfWeek.SATURDAY, LocalDate.now());
    }

    public static LocalDate nextSunday() {
        return getNextDate(DayOfWeek.SATURDAY, LocalDate.now());
    }

}
