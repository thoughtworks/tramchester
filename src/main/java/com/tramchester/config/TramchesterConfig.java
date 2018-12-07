package com.tramchester.config;

import io.dropwizard.Configuration;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;

import java.nio.file.Path;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

public abstract class TramchesterConfig extends Configuration {

    public static ZoneId TimeZone = ZoneId.of("Europe/London");

    // do full rebuild of the graph DB
    public abstract boolean getRebuildGraph();

    // pull new copy of timetable data
    public abstract boolean getPullData();

    // name of the graph DB to use
    public abstract String getGraphName();

    // a list of currently close stations
    public abstract List<String> getClosedStations();

    // transport agencies to load data for, * means all
    public abstract Set<String> getAgencies();

    // URL to pull Cloud instance meta-data from
    public abstract String getInstanceDataUrl();

    // url to load timetable data from
    public abstract String getTramDataUrl();

    // where to load timetable data from and place preprocessed data
    public abstract Path getDataPath();

    public abstract int getTimeWindow();

    // range to scan for nearby stations
    public abstract Double getNearestStopRangeKM();

    // limit on number of near stops to consider
    public abstract int getNumOfNearestStops();

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

    // how often to query for trams into the future from initial query tie, i.e. every 6 minutes
    public abstract int getQueryInterval();

    // how many stops show in Recent on the stations dropdowns
    public abstract int getRecentStopsToShow();

    public abstract SwaggerBundleConfiguration getSwaggerBundleConfiguration();

    // number of days before data expiry to start warning
    public abstract int getDataExpiryThreadhold();

    // URL for live tram data
    public abstract String getLiveDataUrl();

    // access key for live data URL issued by TFGM
    public abstract String getLiveDataSubscriptionKey();

    // name of AWS S3 bucket where live data is archived
    public abstract String getLiveDataS3Bucket();

    // Edge per trip/time instead of an array of time
    public abstract boolean getEdgePerTrip();
}
