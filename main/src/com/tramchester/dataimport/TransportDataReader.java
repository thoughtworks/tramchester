package com.tramchester.dataimport;


import com.tramchester.dataimport.data.*;
import com.tramchester.dataimport.parsers.*;
import com.tramchester.domain.FeedInfo;

import java.nio.file.Path;
import java.util.stream.Stream;

public class TransportDataReader {
    public static final String TRIPS = "trips";
    public static final String STOPS = "stops";
    public static final String ROUTES = "routes";
    public static final String FEED_INFO = "feed_info";
    public static String CALENDAR = "calendar";
    public static String STOP_TIMES = "stop_times";

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
        return new DataLoader<>(formPath(CALENDAR), new CalendarDataParser()).loadAll(skipHeader);
    }

    public Stream<StopTimeData> getStopTimes() {
        return new DataLoader<>(formPath(STOP_TIMES), new StopTimeDataParser()).loadAll(skipHeader);
    }

    public Stream<TripData> getTrips() {
        return new DataLoader<>(formPath(TRIPS), new TripDataParser()).loadAll(skipHeader);
    }

    public Stream<StopData> getStops() {
        return new DataLoader<>(formPath(STOPS), new StopDataParser()).loadAll(skipHeader);
    }

    public Stream<RouteData> getRoutes() {
        return new DataLoader<>(formPath(ROUTES), new RouteDataParser()).loadAll(skipHeader);
    }

    public Stream<FeedInfo> getFeedInfo() {
        return new DataLoader<>(formPath(FEED_INFO), new FeedInfoDataParser()).loadAll(skipHeader);
    }
}
