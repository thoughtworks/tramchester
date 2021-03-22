package com.tramchester.config;

import com.tramchester.domain.StationClosure;
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

public abstract class TramchesterConfig extends Configuration {

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
    public abstract boolean getRemoveRouteNameSuffix();

    // only allow changing vehicles at interchanges
    public abstract boolean getChangeAtInterchangeOnly();

    // load postcodes?
    public abstract boolean getLoadPostcodes();

    // add neighbouring stations
    public abstract boolean getCreateNeighbours();

    // distance for neighbouring stations, in KM
    public abstract double getDistanceToNeighboursKM() ;

    // unpacked location of postcode data
    public abstract Path getPostcodeDataPath();

    // location of zip file containing postcode
    public abstract Path getPostcodeZip();

    // config for each of the data sources
    public abstract List<DataSourceConfig> getDataSourceConfig();

    // a list of currently close stations
    public abstract List<StationClosure> getStationClosures();

    // live transport data config
    public abstract LiveDataConfig getLiveDataConfig();

    // Graph DB Config
    public abstract GraphDBConfig getGraphDBConfig();

    // bounding box for stations to include
    public abstract BoundingBox getBounds();

    public Set<TransportMode> getTransportModes() {
        return getDataSourceConfig().stream().
                map(DataSourceConfig::getTransportModes).
                flatMap(Collection::stream).
                map(TransportMode::fromGTFS).
                collect(Collectors.toSet());
    }

    // number of connections to make by walking
    public abstract int getMaxWalkingConnections();
}
