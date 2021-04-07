package com.tramchester.repository;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.dataimport.data.*;
import com.tramchester.domain.DataSourceInfo;
import com.tramchester.domain.FeedInfo;
import com.tramchester.domain.factory.TransportEntityFactory;

import java.util.stream.Stream;

public class TransportDataSource {
    final Stream<StopData> stops;
    final Stream<RouteData> routes;
    final Stream<TripData> trips;
    final Stream<StopTimeData> stopTimes;
    final Stream<CalendarData> calendars;
    final Stream<FeedInfo> feedInfo;
    final Stream<CalendarDateData> calendarsDates;
    private final GTFSSourceConfig config;
    final Stream<AgencyData> agencies;
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

    public DataSourceInfo getDataSourceInfo() {
        return dataSourceInfo;
    }

    public GTFSSourceConfig getConfig() {
        return config;
    }

    public TransportEntityFactory getEntityFactory() {
        return entityFactory;
    }
}
