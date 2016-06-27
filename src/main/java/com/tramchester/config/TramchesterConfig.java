package com.tramchester.config;

import io.dropwizard.Configuration;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public abstract class TramchesterConfig extends Configuration {
    // do full rebuild of the graph DB
    public abstract boolean isRebuildGraph();
    // pull new copy of timetable data
    public abstract boolean isPullData();
    // redo filtering of the timetable data
    public abstract boolean isFilterData();

    // name of the graph DB to use
    public abstract String getGraphName();
    // a list of currently close stations
    public abstract List<String> getClosedStations();
    // transport agencies to load data for, * means all
    public abstract Set<String> getAgencies();

    // URL to pull Cloud instance meta-data from
    public abstract String getInstanceDataBaseURL();
    // AWS Region to use for cloudwatch metrics
    public abstract String getAWSRegionName();

    // url to load timetable data from
    public abstract String getTramDataUrl();
    // where to load timetable data from
    public abstract Path getInputDataPath();
    // where to place preprocessed timetable data
    public abstract Path getOutputDataPath();
    // use generic mapper, i.e. not tram specific one

    public abstract int getTimeWindow();
    // show users 'My Location' in stops list
    public abstract boolean showMyLocation();
    // range to scan for nearby stations
    public abstract Double getNearestStopRangeKM();
    // limit on number of near stops to consider
    public abstract int getNumOfNearestStops();
    // an assumed mph for walking
    public abstract double getWalkingMPH();
    // direct traffic from http to https
    public abstract boolean isRedirectHTTP();
    // the secure host, the one the certificate matches
    public abstract String getSecureHost();
    // add in the cross city walking routes to the graph
    public abstract boolean addWalkingRoutes();

    // EXPERIMENTAL FOR BUSES /////////

    // try to create walking links between stations close together
    public abstract boolean isCreateLocality();
    // time window in which to look ahead for journeys from a station during route building
    public abstract boolean useGenericMapper();


}

