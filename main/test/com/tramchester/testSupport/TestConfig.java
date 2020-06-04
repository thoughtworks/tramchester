package com.tramchester.testSupport;

import com.tramchester.config.AppConfiguration;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.server.ServerFactory;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static java.util.Arrays.asList;

public abstract class TestConfig extends AppConfiguration {

    @Override
    public boolean getChangeAtInterchangeOnly() { return true; }

    @Override
    public boolean getRebuildGraph() {
        return false;
    }

    @Override
    public List<String> getClosedStations() {
        return closedStations;
    }

    //////
    private final List<String> closedStations = asList("St Peters Square");

    @Override
    public String getInstanceDataUrl() {
        return "http://localhost:8080";
    }

    @Override
    public String getTramDataUrl() {
        return "http://odata.tfgm.com/opendata/downloads/TfGMgtfs.zip";
    }

    @Override
    public Path getUnzipPath() {
        return Paths.get("gtdf-out");
    }

    @Override
    public Path getDataPath() {
        return getDataFolder();
    }

    @Override
    public Double getNearestStopRangeKM() {
        return 2D;
    }

    @Override
    public int getNumOfNearestStops() {
        return 6;
    }

    @Override
    public int getNumOfNearestStopsForWalking() {
        return 3;
    }

    @Override
    public double getWalkingMPH() {
        return 3;
    }

    public abstract Path getDataFolder();

    @Override
    public String getSecureHost() {
        return "tramchester.com";
    }

    @Override
    public int getMaxWait() {
        return 25;
    }

    // see RouteCalculatorTest.shouldFindEndOfLinesToEndOfLines
    @Override
    public int getMaxJourneyDuration() { return 112; }

    @Override
    public int getNumberQueries() { return 3; }

    @Override
    public int getQueryInterval() { return 12; }

    @Override
    public int getRecentStopsToShow() {
        return 5;
    }

    @Override
    public int getMaxNumResults() {
        return 5;
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
    public long getLiveDataRefreshPeriodSeconds() { return 20L; }

    @Override
    public boolean getRemoveRouteNameSuffix() {
        // issue with truncation of source data, until this is fixed this needs to remain true
        return true;
    }

    @Override
    public int getMaxNumberMissingLiveMessages() {
        return 5;
    }

    @Override
    public boolean getBus() {
        return false;
    }

    @Override
    public Path getPostcodeDataPath() {
        return Path.of("data", "codepo_gb", "Data", "CSV");
    }

    @Override
    public Path getPostcodeZip() {
        return Path.of("data", "codepo_gb.zip");
    }

    @Override
    public ServerFactory getServerFactory() {
        DefaultServerFactory factory = new DefaultServerFactory();
        factory.setApplicationContextPath("/");
        factory.setAdminContextPath("/admin");
        factory.setJerseyRootPath("/api/*");
        return factory;
    }


}
