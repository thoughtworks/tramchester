package com.tramchester.config;

import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.BoundingBox;
import io.dropwizard.Configuration;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;

import java.nio.file.Path;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class TramchesterConfig extends Configuration implements HasRemoteDataSourceConfig, HasGraphDBConfig {

    public static final ZoneId TimeZone = ZoneId.of("Europe/London");

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

    // maximum length of a journey
    public abstract int getMaxJourneyDuration();

    public abstract SwaggerBundleConfiguration getSwaggerBundleConfiguration();

    // number of days before data expiry to start warning
    public abstract int getDataExpiryThreadhold();

    // remove the (Purple Line) part of the route name?
    //public abstract boolean getRemoveRouteNameSuffix();

    // only allow changing vehicles at interchanges
    public abstract boolean getChangeAtInterchangeOnly();

    // add neighbouring stations
    public abstract boolean getCreateNeighbours();

    // distance for neighbouring stations, in KM
    public abstract double getDistanceToNeighboursKM() ;

    // config for each of the GTFS data sources
    public abstract List<GTFSSourceConfig> getGTFSDataSource();

    // config for each remote data source to be downloaded
    public abstract List<RemoteDataSourceConfig> getRemoteDataSourceConfig();

    // live transport data config
    public abstract LiveDataConfig getLiveDataConfig();

    // Graph DB Config
    public abstract GraphDBConfig getGraphDBConfig();

    // bounding box for stations to include
    public abstract BoundingBox getBounds();

    public Set<TransportMode> getTransportModes() {
        return getGTFSDataSource().stream().
                map(GTFSSourceConfig::getTransportModes).
                flatMap(Collection::stream).
                collect(Collectors.toSet());
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

    // number of direct walks between stations
    public abstract int getMaxNeighbourConnections();

    public abstract boolean getSendCloudWatchMetrics();

    public boolean liveDataEnabled() {
        return getLiveDataConfig()!=null;
    }

    public final boolean getDepthFirst() {
        return true;
    }

    public abstract Path getCacheFolder();

    public abstract long getCalcTimeoutMillis();

    public abstract long GetCloudWatchMetricsFrequencyMinutes();

    public abstract boolean getPlanningEnabled();
}
