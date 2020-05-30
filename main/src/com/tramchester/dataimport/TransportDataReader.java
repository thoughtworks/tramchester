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
    private final boolean skipHeaders;

    public TransportDataReader(DataLoaderFactory factory, boolean skipHeaders) {
        this.factory = factory;
        this.skipHeaders = skipHeaders;
    }

    public Stream<CalendarData> getCalendar(CalendarDataMapper calendarDataMapper) {
        return factory.getLoaderFor(InputFiles.calendar, calendarDataMapper).loadFiltered(skipHeaders);
    }

    public Stream<CalendarDateData> getCalendarDates(CalendarDatesDataMapper calendarDatesMapper) {
        return factory.getLoaderFor(InputFiles.calendar_dates, calendarDatesMapper).loadFiltered(skipHeaders);
    }

    public Stream<StopTimeData> getStopTimes(StopTimeDataMapper stopTimeDataMapper) {
        return factory.getLoaderFor(InputFiles.stop_times, stopTimeDataMapper).loadFiltered(skipHeaders);
    }

    public Stream<TripData> getTrips(TripDataMapper tripDataMapper) {
        return factory.getLoaderFor(InputFiles.trips, tripDataMapper).loadFiltered(skipHeaders);
    }

    public Stream<StopData> getStops(StopDataMapper stopDataMapper) {
        return factory.getLoaderFor(InputFiles.stops, stopDataMapper).loadFiltered(skipHeaders);
    }

    public Stream<RouteData> getRoutes(RouteDataMapper routeDataMapper) {
        return factory.getLoaderFor(InputFiles.routes, routeDataMapper).loadFiltered(skipHeaders);
    }

    public Stream<FeedInfo> getFeedInfo(FeedInfoDataMapper feedInfoDataMapper) {
        return factory.getLoaderFor(InputFiles.feed_info, feedInfoDataMapper).loadFiltered(skipHeaders);
    }
}
