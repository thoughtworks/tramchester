package com.tramchester.dataimport.loader;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.dataimport.data.*;
import com.tramchester.domain.DataSourceInfo;
import com.tramchester.domain.FeedInfo;
import com.tramchester.domain.factory.TransportEntityFactory;

import java.util.stream.Stream;

public class TransportDataSource {
    private final Stream<StopData> stops;
    private final Stream<RouteData> routes;
    private final Stream<TripData> trips;
    private final Stream<StopTimeData> stopTimes;
    private final Stream<CalendarData> calendars;
    private final Stream<CalendarDateData> calendarsDates;
    private final Stream<AgencyData> agencies;
    private final Stream<FeedInfo> feedInfo;

    private final GTFSSourceConfig config;
    final private DataSourceInfo dataSourceInfo;
    final private TransportEntityFactory entityFactory;

    public TransportDataSource(DataSourceInfo dataSourceInfo, Stream<AgencyData> agencies, Stream<StopData> stops,
                               Stream<RouteData> routes, Stream<TripData> trips, Stream<StopTimeData> stopTimes,
                               Stream<CalendarData> calendars,
                               Stream<FeedInfo> feedInfo, Stream<CalendarDateData> calendarsDates,
                               GTFSSourceConfig config, TransportEntityFactory entityFactory) {
        this.dataSourceInfo = dataSourceInfo;
        this.agencies = agencies;
        this.stops = stops;
        this.routes = routes;
        this.trips = trips;
        this.stopTimes = stopTimes;
        this.calendars = calendars;
        this.feedInfo = feedInfo;
        this.calendarsDates = calendarsDates;
        this.config = config;
        this.entityFactory = entityFactory;
    }

    public void closeAll() {
        stops.close();
        routes.close();
        trips.close();
        stopTimes.close();
        calendars.close();
        feedInfo.close();
        calendarsDates.close();
        agencies.close();
    }

    public Stream<FeedInfo> getFeedInfoStream() {
        return feedInfo;
    }

    public DataSourceInfo getDataSourceInfo() {
        return dataSourceInfo;
    }

    public GTFSSourceConfig getConfig() {
        return config;
    }

    public TransportEntityFactory getEntityFactory() {
        return entityFactory;
    }

    public Stream<StopData> getStops() {
        return stops;
    }

    public Stream<AgencyData> getAgencies() {
        return agencies;
    }

    public Stream<RouteData> getRoutes() {
        return routes;
    }

    public Stream<TripData> getTrips() {
        return trips;
    }

    public Stream<StopTimeData> getStopTimes() {
        return stopTimes;
    }

    public Stream<CalendarData> getCalendars() {
        return calendars;
    }

    public Stream<CalendarDateData> getCalendarsDates() {
        return calendarsDates;
    }
}
