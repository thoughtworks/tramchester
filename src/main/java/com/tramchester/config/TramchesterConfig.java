package com.tramchester.config;

import io.dropwizard.Configuration;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public abstract class TramchesterConfig extends Configuration {
    // do full rebuild of the graph DB
    public abstract boolean getRebuildGraph();
    // pull new copy of timetable data
    public abstract boolean getPullData();
    // redo filtering of the timetable data
    public abstract boolean getFilterData();

    // name of the graph DB to use
    public abstract String getGraphName();
    // a list of currently close stations
    public abstract List<String> getClosedStations();
    // transport agencies to load data for, * means all
    public abstract Set<String> getAgencies();

    // URL to pull Cloud instance meta-data from
    public abstract String getInstanceDataUrl();
    // AWS Region to use for cloudwatch metrics
    public abstract String getAwsRegionName();

    // url to load timetable data from
    public abstract String getTramDataUrl();

    // where to load timetable data from and place preprocessed data
    public abstract Path getDataPath();

    public abstract int getTimeWindow();

    // show users 'My Location' in stops list
    public abstract boolean getShowMyLocation();

    // range to scan for nearby stations
    public abstract Double getNearestStopRangeKM();
    // limit on number of near stops to consider
    public abstract int getNumOfNearestStops();
    // an assumed mph for walking
    public abstract double getWalkingMPH();
    // direct traffic from http to https
    public abstract boolean getRedirectHTTP();
    // the secure host, the one the certificate matches
    public abstract String getSecureHost();
    // add in the cross city walking routes to the graph
    public abstract boolean getAddWalkingRoutes();

    // EXPERIMENTAL FOR BUSES /////////

    // try to create walking links between stations close together
    public abstract boolean getCreateLocality();

    // max time to wait for tram/connection
    public abstract int getMaxWait();

    // how often to query for trams into the future from initial query tie, i.e. every 6 minutes
    public abstract int getQueryInterval();

    // how many stops show in Recent on the stations dropdowns
    public abstract int getRecentStopsToShow();
}

