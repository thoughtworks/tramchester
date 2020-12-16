package com.tramchester.dataimport;


import com.tramchester.config.DataSourceConfig;
import com.tramchester.dataimport.data.*;
import com.tramchester.dataimport.parsers.*;
import com.tramchester.domain.FeedInfo;
import com.tramchester.domain.DataSourceInfo;

import java.util.stream.Stream;

public class TransportDataReader {

    private final DataSourceInfo dataSourceInfo;
    private final DataSourceConfig config;

    public DataSourceConfig getConfig() {
        return config;
    }

    public enum InputFiles {
        trips, stops, routes, feed_info, calendar, stop_times, calendar_dates, agency
    }

    private final DataLoaderFactory factory;

    public TransportDataReader(DataSourceInfo dataSourceInfo, DataLoaderFactory factory, DataSourceConfig config) {
        this.dataSourceInfo = dataSourceInfo;
        this.factory = factory;
        this.config = config;
    }

    public DataSourceInfo getNameAndVersion() {
        return dataSourceInfo;
    }

    public Stream<CalendarData> getCalendar(CalendarDataMapper calendarDataMapper) {
        return factory.getLoaderFor(InputFiles.calendar, calendarDataMapper).load();
    }

    public Stream<CalendarDateData> getCalendarDates(CalendarDatesDataMapper calendarDatesMapper) {
        return factory.getLoaderFor(InputFiles.calendar_dates, calendarDatesMapper).load();
    }

    public Stream<StopTimeData> getStopTimes() {
        return factory.getLoaderFor(InputFiles.stop_times, StopTimeData.class).load();
    }

    public Stream<TripData> getTrips(TripDataMapper tripDataMapper) {
        return factory.getLoaderFor(InputFiles.trips, tripDataMapper).load();
    }

    public Stream<StopData> getStops(StopDataMapper stopDataMapper) {
        return factory.getLoaderFor(InputFiles.stops, stopDataMapper).load();
    }

    public Stream<RouteData> getRoutes(RouteDataMapper routeDataMapper) {
        return factory.getLoaderFor(InputFiles.routes, routeDataMapper).load();
    }

    public Stream<FeedInfo> getFeedInfo(FeedInfoDataMapper feedInfoDataMapper) {
        return factory.getLoaderFor(InputFiles.feed_info, feedInfoDataMapper).load();
    }

    public Stream<AgencyData> getAgencies(AgencyDataMapper agencyDataMapper) {
        return factory.getLoaderFor(InputFiles.agency, agencyDataMapper).load();
    }

}
