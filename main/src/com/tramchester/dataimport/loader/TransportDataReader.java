package com.tramchester.dataimport.loader;


import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.dataimport.data.*;
import com.tramchester.domain.FeedInfo;
import com.tramchester.domain.DataSourceInfo;

import java.util.stream.Stream;

public class TransportDataReader {

    private final DataSourceInfo dataSourceInfo;
    private final GTFSSourceConfig config;

    public GTFSSourceConfig getConfig() {
        return config;
    }

    public enum InputFiles {
        trips, stops, routes, feed_info, calendar, stop_times, calendar_dates, agency
    }

    private final TransportDataFromFileFactory factory;

    public TransportDataReader(DataSourceInfo dataSourceInfo, TransportDataFromFileFactory factory, GTFSSourceConfig config) {
        this.dataSourceInfo = dataSourceInfo;
        this.factory = factory;
        this.config = config;
    }

    public DataSourceInfo getNameAndVersion() {
        return dataSourceInfo;
    }

    public Stream<CalendarData> getCalendar() {
        return factory.getLoaderFor(InputFiles.calendar, CalendarData.class).load();
    }

    public Stream<CalendarDateData> getCalendarDates() {
        return factory.getLoaderFor(InputFiles.calendar_dates, CalendarDateData.class).load();
    }

    public Stream<StopTimeData> getStopTimes() {
        return factory.getLoaderFor(InputFiles.stop_times, StopTimeData.class).load();
    }

    public Stream<TripData> getTrips() {
        return factory.getLoaderFor(InputFiles.trips, TripData.class).load();
    }

    public Stream<StopData> getStops() {
        return factory.getLoaderFor(InputFiles.stops, StopData.class).load();
    }

    public Stream<RouteData> getRoutes() {
        return factory.getLoaderFor(InputFiles.routes, RouteData.class).load();
    }

    public Stream<FeedInfo> getFeedInfo() {
        return factory.getLoaderFor(InputFiles.feed_info, FeedInfo.class).load();
    }

    public Stream<AgencyData> getAgencies() {
        return factory.getLoaderFor(InputFiles.agency, AgencyData.class).load();
    }

}
