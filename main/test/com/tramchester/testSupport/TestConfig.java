package com.tramchester.testSupport;

import com.tramchester.config.AppConfiguration;
import com.tramchester.config.DataSourceConfig;
import com.tramchester.config.LiveDataAppConfig;
import com.tramchester.config.LiveDataConfig;
import com.tramchester.domain.StationClosure;
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
    public List<DataSourceConfig> getDataSourceConfig() {
        return getDataSourceFORTESTING();
    }

    protected abstract List<DataSourceConfig> getDataSourceFORTESTING();

    @Override
    public boolean getChangeAtInterchangeOnly() { return true; }

    @Override
    public List<StationClosure> getStationClosures() {
        return Collections.emptyList();
    }

    @Override
    public String getInstanceDataUrl() {
        return "http://localhost:8080";
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

    // see RouteCalculatorTest.shouldFindEndOfLinesToEndOfLines
    @Override
    public int getMaxJourneyDuration() { return 122; }

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
    public boolean getRemoveRouteNameSuffix() {
        // issue with truncation of source data, until this is fixed this needs to remain true
        return true;
    }

    @Override
    public boolean getLoadPostcodes() {
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

    @Override
    public boolean getCreateNeighbours() {
        return false;
    }

    @Override
    public double getDistanceToNeighboursKM() {
        return 0.4;
    }

    @Override
    public @Valid BoundingBox getBounds() {
        return TestEnv.getTFGMBusBounds();
    }

    @Override
    public LiveDataConfig getLiveDataConfig() {
        return new TestLiveDataConfig();
    }
}
