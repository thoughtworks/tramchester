package com.tramchester.dataimport;


import com.tramchester.dataimport.data.*;
import com.tramchester.dataimport.parsers.*;
import com.tramchester.domain.DataSourceInfo;
import com.tramchester.domain.FeedInfo;

import java.util.stream.Stream;

public class TransportDataReader {

    private final DataSourceInfo.NameAndVersion nameAndVersion;

    public enum InputFiles {
        trips, stops, routes, feed_info, calendar, stop_times, calendar_dates, agency
    }

    private final DataLoaderFactory factory;
    private final boolean expectFeedinfo;

    public TransportDataReader(DataSourceInfo.NameAndVersion nameAndVersion, DataLoaderFactory factory, boolean expectFeedinfo) {
        this.nameAndVersion = nameAndVersion;
        this.factory = factory;
        this.expectFeedinfo = expectFeedinfo;
    }

    public DataSourceInfo.NameAndVersion getNameAndVersion() {
        return nameAndVersion;
    }

    public Stream<CalendarData> getCalendar(CalendarDataMapper calendarDataMapper) {
        return factory.getLoaderFor(InputFiles.calendar, calendarDataMapper, true).loadFiltered(true);
    }

    public Stream<CalendarDateData> getCalendarDates(CalendarDatesDataMapper calendarDatesMapper) {
        return factory.getLoaderFor(InputFiles.calendar_dates, calendarDatesMapper, true).loadFiltered(true);
    }

    public Stream<StopTimeData> getStopTimes(StopTimeDataMapper stopTimeDataMapper) {
        return factory.getLoaderFor(InputFiles.stop_times, stopTimeDataMapper, true).loadFiltered(true);
    }

    public Stream<TripData> getTrips(TripDataMapper tripDataMapper) {
        return factory.getLoaderFor(InputFiles.trips, tripDataMapper, true).loadFiltered(true);
    }

    public Stream<StopData> getStops(StopDataMapper stopDataMapper) {
        return factory.getLoaderFor(InputFiles.stops, stopDataMapper, true).loadFiltered(true);
    }

    public Stream<RouteData> getRoutes(RouteDataMapper routeDataMapper) {
        return factory.getLoaderFor(InputFiles.routes, routeDataMapper, true).loadFiltered(true);
    }

    public Stream<FeedInfo> getFeedInfo(FeedInfoDataMapper feedInfoDataMapper) {
        return factory.getLoaderFor(InputFiles.feed_info, feedInfoDataMapper, true).loadFiltered(true);
    }

    public Stream<AgencyData> getAgencies(AgencyDataMapper agencyDataMapper) {
        return factory.getLoaderFor(InputFiles.agency, agencyDataMapper, true).loadFiltered(true);
    }

    public boolean getExpectFeedinfo() {
        return expectFeedinfo;
    }
}
