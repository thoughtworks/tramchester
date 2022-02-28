package com.tramchester.testSupport;

import com.tramchester.config.AppConfiguration;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.LiveDataConfig;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.geo.BoundingBox;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.server.ServerFactory;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;

import javax.validation.Valid;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public abstract class TestConfig extends AppConfiguration {

    @Override
    public List<GTFSSourceConfig> getGTFSDataSource() {
        return getDataSourceFORTESTING();
    }

    protected abstract List<GTFSSourceConfig> getDataSourceFORTESTING();

    @Override
    public boolean getChangeAtInterchangeOnly() { return true; }

    @Override
    public boolean getSendCloudWatchMetrics() {
        return false;
    }

    @Override
    public long GetCloudWatchMetricsFrequencyMinutes() {
        return 10;
    }

    @Override
    public Integer getStaticAssetCacheTimeSeconds() {
        return 5*60;
    }

    @Override
    public String getInstanceDataUrl() {
        return "";
    }

    @Override
    public Double getNearestStopRangeKM() {
        return 1.6D;
    }

    @Override
    public Double getNearestStopForWalkingRangeKM() {
        return 1.6D;
    }

    @Override
    public int getNumOfNearestStopsToOffer() {
        return 5;
    }

    @Override
    public int getNumOfNearestStopsForWalking() {
        return 3;
    }

    @Override
    public double getWalkingMPH() {
        return 3;
    }

    @Override
    public String getSecureHost() {
        return "tramchester.com";
    }

    @Override
    public int getMaxWait() {
        return 25;
    }

    @Override
    public int getMaxInitialWait() {
        return 13;
    }

    // see RouteCalculatorTest.shouldFindEndOfLinesToEndOfLines
    @Override
    public int getMaxJourneyDuration() { return 124; }

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
    public long getCalcTimeoutMillis() {
        return 3500;
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
    public ServerFactory getServerFactory() {
        DefaultServerFactory factory = new DefaultServerFactory();
        factory.setApplicationContextPath("/");
        factory.setAdminContextPath("/admin");
        factory.setJerseyRootPath("/api/*");
        return factory;
    }

    @Override
    public @Valid BoundingBox getBounds() {
        return TestEnv.getTFGMBusBounds();
    }

    @Override
    public LiveDataConfig getLiveDataConfig() {
        return null;
    }

    @Override
    public int getMaxWalkingConnections() {
        return 2;
    }

    @Override
    public List<RemoteDataSourceConfig> getRemoteDataSourceConfig() {
        return Collections.emptyList();
    }

    @Override
    public Path getCacheFolder() {
        throw new RuntimeException("Must define");
    }

    @Override
    public boolean getPlanningEnabled() {
        return true;
    }

    @Override
    public boolean hasNeighbourConfig() {
        return false;
    }

    @Override
    public String getDistributionBucket() {
        return "tramchester2dist";
    }
}
