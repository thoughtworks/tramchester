package com.tramchester.dataimport;


import com.tramchester.dataimport.data.*;
import com.tramchester.dataimport.parsers.*;
import com.tramchester.domain.FeedInfo;

import java.nio.file.Path;
import java.util.stream.Stream;

public class TransportDataReader {
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
        return new DataLoader<>(formPath("calendar"), new CalendarDataParser()).loadAll(skipHeader);
    }

    public Stream<StopTimeData> getStopTimes() {
        return new DataLoader<>(formPath("stop_times"), new StopTimeDataParser()).loadAll(skipHeader);
    }

    public Stream<TripData> getTrips() {
        return new DataLoader<>(formPath("trips"), new TripDataParser()).loadAll(skipHeader);
    }

    public Stream<StopData> getStops() {
        return new DataLoader<>(formPath("stops"), new StopDataParser()).loadAll(skipHeader);
    }

    public Stream<RouteData> getRoutes() {
        return new DataLoader<>(formPath("routes"), new RouteDataParser()).loadAll(skipHeader);
    }

    public Stream<FeedInfo> getFeedInfo() {
        return new DataLoader<>(formPath("feed_info"), new FeedInfoDataParser()).loadAll(skipHeader);
    }
}
