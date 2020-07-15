package com.tramchester.dataimport;


import com.tramchester.dataimport.data.*;
import com.tramchester.dataimport.parsers.*;
import com.tramchester.domain.FeedInfo;

import java.util.stream.Stream;

public class TransportDataReader {

    public enum InputFiles {
        trips, stops, routes, feed_info, calendar, stop_times, calendar_dates;
    }

    private final DataLoaderFactory factory;

    @Deprecated
    private final boolean skipFirstLine;

    @Deprecated
    public TransportDataReader(DataLoaderFactory factory, boolean skipFirstLine) {
        this.factory = factory;
        this.skipFirstLine = skipFirstLine;
    }

    public Stream<CalendarData> getCalendar(CalendarDataMapper calendarDataMapper) {
        return factory.getLoaderFor(InputFiles.calendar, calendarDataMapper, true).loadFiltered(skipFirstLine);
    }

    public Stream<CalendarDateData> getCalendarDates(CalendarDatesDataMapper calendarDatesMapper) {
        return factory.getLoaderFor(InputFiles.calendar_dates, calendarDatesMapper, true).loadFiltered(skipFirstLine);
    }

    public Stream<StopTimeData> getStopTimes(StopTimeDataMapper stopTimeDataMapper) {
        return factory.getLoaderFor(InputFiles.stop_times, stopTimeDataMapper, true).loadFiltered(skipFirstLine);
    }

    public Stream<TripData> getTrips(TripDataMapper tripDataMapper) {
        return factory.getLoaderFor(InputFiles.trips, tripDataMapper, true).loadFiltered(skipFirstLine);
    }

    public Stream<StopData> getStops(StopDataMapper stopDataMapper) {
        return factory.getLoaderFor(InputFiles.stops, stopDataMapper, true).loadFiltered(true);
    }

    public Stream<RouteData> getRoutes(RouteDataMapper routeDataMapper) {
        return factory.getLoaderFor(InputFiles.routes, routeDataMapper, true).loadFiltered(skipFirstLine);
    }

    public Stream<FeedInfo> getFeedInfo(boolean mandatory, FeedInfoDataMapper feedInfoDataMapper) {
        return factory.getLoaderFor(InputFiles.feed_info, feedInfoDataMapper, mandatory).loadFiltered(skipFirstLine);
    }
}
