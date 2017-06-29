package com.tramchester.integration.dataimport;


import com.tramchester.integration.dataimport.data.*;
import com.tramchester.integration.dataimport.parsers.*;
import com.tramchester.domain.FeedInfo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

public class TransportDataReader {
    private final Path path;

    public TransportDataReader(Path path) {
        this.path = path;
    }

    private String formPath(String filename) {
        return path.resolve(filename).toAbsolutePath().toString();
    }

    public Stream<CalendarData> getCalendar() throws IOException {
        return new DataLoader<>(formPath("calendar"), new CalendarDataParser()).loadAll();
    }

    public Stream<StopTimeData> getStopTimes() throws IOException {
        return new DataLoader<>(formPath("stop_times"), new StopTimeDataParser()).loadAll();
    }

    public Stream<TripData> getTrips() throws IOException {
        return new DataLoader<>(formPath("trips"), new TripDataParser()).loadAll();
    }

    public Stream<StopData> getStops() throws IOException {
        return new DataLoader<>(formPath("stops"), new StopDataParser()).loadAll();
    }

    public Stream<RouteData> getRoutes() throws IOException {
        return new DataLoader<>(formPath("routes"), new RouteDataParser()).loadAll();
    }

    public Stream<FeedInfo> getFeedInfo() throws IOException {
        return new DataLoader<>(formPath("feed_info"), new FeedInfoDataParser()).loadAll();
    }
}
