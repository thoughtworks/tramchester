package com.tramchester.dataimport;


import com.tramchester.dataimport.data.*;
import com.tramchester.dataimport.parsers.*;
import com.tramchester.domain.FeedInfo;

import java.nio.file.Path;
import java.util.stream.Stream;

public class TransportDataReader {

    public enum InputFiles {
        trips, stops, routes, feed_info, calendar, stop_times;
    }

//    public static final String TRIPS = "trips";
//    public static final String STOPS = "stops";
//    public static final String ROUTES = "routes";
//    public static final String FEED_INFO = "feed_info";
//    public static String CALENDAR = "calendar";
//    public static String STOP_TIMES = "stop_times";

    private final Path path;
    private boolean skipHeader;

    public TransportDataReader(Path path, boolean skipHeader) {
        this.path = path;
        this.skipHeader = skipHeader;
    }

    private String formPath(String filename) {
        return path.resolve(filename).toAbsolutePath().toString();
    }

    public Stream<CalendarData> getCalendar() {
        return new DataLoader<>(formPath(InputFiles.calendar.name()), new CalendarDataParser()).loadAll(skipHeader);
    }

    public Stream<StopTimeData> getStopTimes() {
        return new DataLoader<>(formPath(InputFiles.stop_times.name()), new StopTimeDataParser()).loadAll(skipHeader);
    }

    public Stream<TripData> getTrips() {
        return new DataLoader<>(formPath(InputFiles.trips.name()), new TripDataParser()).loadAll(skipHeader);
    }

    public Stream<StopData> getStops() {
        return new DataLoader<>(formPath(InputFiles.stops.name()), new StopDataParser()).loadAll(skipHeader);
    }

    public Stream<RouteData> getRoutes() {
        return new DataLoader<>(formPath(InputFiles.routes.name()), new RouteDataParser()).loadAll(skipHeader);
    }

    public Stream<FeedInfo> getFeedInfo() {
        return new DataLoader<>(formPath(InputFiles.feed_info.name()), new FeedInfoDataParser()).loadAll(skipHeader);
    }
}
