package com.tramchester.config;

import io.dropwizard.Configuration;
import io.dropwizard.bundles.assets.AssetsBundleConfiguration;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;

import java.nio.file.Path;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

public abstract class TramchesterConfig extends Configuration implements AssetsBundleConfiguration, DownloadConfig {

    public static ZoneId TimeZone = ZoneId.of("Europe/London");

    // do full rebuild of the graph DB
    public abstract boolean getRebuildGraph();

    // name of the graph DB to use
    public abstract String getGraphName();

    // a list of currently close stations
    public abstract List<String> getClosedStations();

    // transport agencies to load data for, empty list [] means all
    public abstract Set<String> getAgencies();

    // are bus routes includes, see also agencies list
    public abstract boolean getBus();

    // URL to pull Cloud instance meta-data from
    public abstract String getInstanceDataUrl();

    // range to scan for nearby stations
    public abstract Double getNearestStopRangeKM();

    // limit on number of near stops to display front-end
    public abstract int getNumOfNearestStops();

    // limit on number of near stops to consider when walking to/from a station
    public abstract int getNumOfNearestStopsForWalking();

    // an assumed mph for walking
    public abstract double getWalkingMPH();

    // direct traffic from http to https (not always true, is set false in deploys to dev)
    public abstract boolean getRedirectHTTP();

    // the secure host, the one the certificate matches
    public abstract String getSecureHost();

    // EXPERIMENTAL FOR BUSES /////////
    // try to create walking links between stations close together
    public abstract boolean getCreateLocality();

    // max time to wait for tram/connection
    public abstract int getMaxWait();

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

    // URL for live tram data
    public abstract String getLiveDataUrl();

    // access key for live data URL issued by TFGM
    public abstract String getLiveDataSubscriptionKey();

    // name of AWS S3 bucket where live data is archived
    public abstract String getLiveDataS3Bucket();

    // remove the (Purple Line) part of the route name?
    public abstract boolean getRemoveRouteNameSuffix();

    // how often to refresh the live data from tfgm
    public abstract long getLiveDataRefreshPeriodSeconds();

    // only allow changing vehicles at interchanges
    public abstract boolean getChangeAtInterchangeOnly();

    // limit on missing messages before triggering healthcheck
    public abstract int getMaxNumberMissingLiveMessages();

    public abstract Path getPostcodeDataPath();
}
