package com.tramchester.config;

import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.BoundingBox;
import io.dropwizard.Configuration;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;

import java.nio.file.Path;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

public abstract class TramchesterConfig extends Configuration implements HasRemoteDataSourceConfig, HasGraphDBConfig {

    public static final ZoneId TimeZoneId = ZoneId.of("Europe/London");

    private final Map<DataSourceID, TransportDataSourceConfig> dataSources;

    protected TramchesterConfig() {
        dataSources = new HashMap<>();
    }

    public abstract Integer getStaticAssetCacheTimeSeconds();

    // URL to pull Cloud instance meta-data from
    public abstract String getInstanceDataUrl();

    // range to scan for nearby stations to display
    public abstract Double getNearestStopRangeKM();

    // range to scan for nearby stations when routing walking to/from
    public abstract Double getNearestStopForWalkingRangeKM();

    // limit on number of near stops to display front-end
    public abstract int getNumOfNearestStopsToOffer();

    // limit on number of near stops to consider when walking to/from a station
    public abstract int getNumOfNearestStopsForWalking();

    // an assumed mph for walking
    public abstract double getWalkingMPH();

    // the secure host, the one the certificate matches
    public abstract String getSecureHost();

    // max time to wait for tram/connection
    public abstract int getMaxWait();

    // max initial time to wait for service
    public abstract int getMaxInitialWait();

    // max number of results to return via the API
    public abstract int getMaxNumResults();

    // number of queries to do for each given time, spaced by QueryInterval below
    public abstract int getNumberQueries();

    // Interval between queryies i.e. every 6 minutes
    public abstract int getQueryInterval();

    // how many stops show in Recent on the stations dropdowns
    public abstract int getRecentStopsToShow();

    // maximum length of a journey in minutes
    public abstract int getMaxJourneyDuration();

    public abstract SwaggerBundleConfiguration getSwaggerBundleConfiguration();

    // number of days before data expiry to start warning
    public abstract int getDataExpiryThreadhold();

    // remove the (Purple Line) part of the route name?
    //public abstract boolean getRemoveRouteNameSuffix();

    // only allow changing vehicles at interchanges
    public abstract boolean getChangeAtInterchangeOnly();

    // neighbours config
    public abstract NeighbourConfig getNeighbourConfig();

    // config for each of the GTFS data sources
    public abstract List<GTFSSourceConfig> getGTFSDataSource();

    // config for each remote data source to be downloaded
    public abstract List<RemoteDataSourceConfig> getRemoteDataSourceConfig();

    // live transport data config
    public abstract TfgmTramLiveDataConfig getLiveDataConfig();

    //  Open Live Departure Boards Web Service config
    public abstract OpenLdbConfig getOpenldbwsConfig();

    // rail data
    public abstract RailConfig getRailConfig();

    // Graph DB Config
    public abstract GraphDBConfig getGraphDBConfig();

    // bounding box for stations to include
    public abstract BoundingBox getBounds();

    public Set<TransportMode> getTransportModes() {
        final Set<TransportMode> modes = getGTFSDataSource().stream().
                map(GTFSSourceConfig::getTransportModes).
                flatMap(Collection::stream).
                collect(Collectors.toSet());

        RailConfig railConfig = getRailConfig();
        if (railConfig!=null) {
            modes.add(TransportMode.Train);
        }
        return modes;
    }

    public RemoteDataSourceConfig getDataRemoteSourceConfig(DataSourceID dataSourceID) {
        return getRemoteDataSourceConfig().stream().
                filter(config -> config.getDataSourceId()==dataSourceID).
                findFirst().orElseThrow();
    }

    public boolean hasRemoteDataSourceConfig(DataSourceID dataSourceID) {
        return getRemoteDataSourceConfig().stream().anyMatch(config -> config.getDataSourceId()==dataSourceID);
    }

    // number of connections to make by walking
    public abstract int getMaxWalkingConnections();

    public abstract boolean getSendCloudWatchMetrics();

    public boolean liveTfgmTramDataEnabled() {
        return getLiveDataConfig()!=null;
    }

    public boolean liveTrainDataEnabled() {
        return getOpenldbwsConfig()!=null;
    }

    public final boolean getDepthFirst() {
        return true;
    }

    public abstract Path getCacheFolder();

    public abstract long getCalcTimeoutMillis();

    public abstract long GetCloudWatchMetricsFrequencyMinutes();

    public abstract boolean getPlanningEnabled();

    public boolean onlyMarkedInterchange(Station station) {
        DataSourceID sourceId = station.getDataSourceID();
        TransportDataSourceConfig sourceConfig = getGetSourceConfigFor(sourceId);
        return sourceConfig.getOnlyMarkedInterchanges();
    }

    private TransportDataSourceConfig getGetSourceConfigFor(DataSourceID sourceId) {
        populateDataSourceMap();
        return dataSources.get(sourceId);
    }

    private void populateDataSourceMap() {
        if (dataSources.isEmpty()) {
            List<GTFSSourceConfig> gtfsSources = getGTFSDataSource();
            gtfsSources.forEach(gtfsSource -> dataSources.put(gtfsSource.getDataSourceId(), gtfsSource));
            RailConfig railConfig = getRailConfig();
            if (railConfig != null) {
                dataSources.put(railConfig.getDataSourceId(), railConfig);
            }
        }
    }

    public abstract boolean hasNeighbourConfig();

    public abstract String getDistributionBucket();
}
