package com.tramchester.dataimport;


import com.tramchester.dataimport.data.*;
import com.tramchester.dataimport.parsers.*;
import com.tramchester.domain.FeedInfo;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

public class TransportDataReader {
    private final String path;

    public TransportDataReader(String path) {
        this.path = path;
    }

    public Stream<CalendarData> getCalendar() throws IOException {
        return new DataLoader<>(path + "calendar", new CalendarDataParser()).loadAll();
    }

    public Stream<StopTimeData> getStopTimes() throws IOException {
        return new DataLoader<>(path + "stop_times", new StopTimeDataParser()).loadAll();
    }

    public Stream<TripData> getTrips() throws IOException {
        return new DataLoader<>(path + "trips", new TripDataParser()).loadAll();
    }

    public Stream<StopData> getStops() throws IOException {
        return new DataLoader<>(path + "stops", new StopDataParser()).loadAll();
    }

    public Stream<RouteData> getRoutes() throws IOException {
        return new DataLoader<>(path + "routes", new RouteDataParser()).loadAll();
    }

    public Stream<FeedInfo> getFeedInfo() throws IOException {
        return new DataLoader<>(path + "feed_info", new FeedInfoDataParser()).loadAll();
    }
}
