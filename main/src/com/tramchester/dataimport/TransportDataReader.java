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

    private final boolean skipHeaders;
    private final Path path;

    public TransportDataReader(Path path, boolean skipHeaders) {
        this.path = path;
        this.skipHeaders = skipHeaders;
    }

    private String formPath(String filename) {
        return path.resolve(filename).toAbsolutePath().toString();
    }

    public Stream<CalendarData> getCalendar() {
        return new DataLoader<>(formPath(InputFiles.calendar.name()), new CalendarDataParser()).loadAll(skipHeaders);
    }

    public Stream<StopTimeData> getStopTimes() {
        return new DataLoader<>(formPath(InputFiles.stop_times.name()), new StopTimeDataParser()).loadAll(skipHeaders);
    }

    public Stream<TripData> getTrips() {
        return new DataLoader<>(formPath(InputFiles.trips.name()), new TripDataParser()).loadAll(skipHeaders);
    }

    public Stream<StopData> getStops() {
        return new DataLoader<>(formPath(InputFiles.stops.name()), new StopDataParser()).loadAll(skipHeaders);
    }

    public Stream<RouteData> getRoutes() {
        return new DataLoader<>(formPath(InputFiles.routes.name()), new RouteDataParser()).loadAll(skipHeaders);
    }

    public Stream<FeedInfo> getFeedInfo() {
        return new DataLoader<>(formPath(InputFiles.feed_info.name()), new FeedInfoDataParser()).loadAll(skipHeaders);
    }
}
