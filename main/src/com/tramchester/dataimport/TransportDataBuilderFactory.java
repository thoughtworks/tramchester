package com.tramchester.dataimport;

import com.tramchester.config.DataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.data.*;
import com.tramchester.dataimport.parsers.*;
import com.tramchester.domain.FeedInfo;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.geo.StationLocations;
import com.tramchester.repository.TransportDataFromFilesBuilderGeoFilter;
import com.tramchester.repository.TransportDataStreams;

import java.util.*;
import java.util.stream.Stream;

public class TransportDataBuilderFactory {

    private final List<TransportDataReader> transportDataReaders;
    private final ProvidesNow providesNow;
    private final StationLocations stationLocations;
    private final TramchesterConfig config;

    public TransportDataBuilderFactory(TransportDataLoader providesLoader, ProvidesNow providesNow,
                                       StationLocations stationLocations, TramchesterConfig config) {
        this.transportDataReaders = providesLoader.getReaders();
        this.providesNow = providesNow;
        this.stationLocations = stationLocations;
        this.config = config;
    }

    // feedinfo is not mandatory in the standard
    public TransportDataFromFilesBuilderGeoFilter create() {
        // streams, so no data read yet

        Set<String> includeAll = Collections.emptySet();
        List<TransportDataStreams> dataStreams = new ArrayList<>();

        transportDataReaders.forEach(transportDataReader -> {
            Stream<FeedInfo> feedInfoData = Stream.empty();
            DataSourceConfig sourceConfig = transportDataReader.getConfig();
            if (sourceConfig.getHasFeedInfo()) {
                feedInfoData = transportDataReader.getFeedInfo(new FeedInfoDataMapper(providesNow));
            }

            Stream<StopData> stopData = transportDataReader.getStops(new StopDataMapper(includeAll));
            Stream<RouteData> routeData = transportDataReader.getRoutes(new RouteDataMapper(includeAll, false));
            Stream<TripData> tripData = transportDataReader.getTrips(new TripDataMapper(includeAll));
            Stream<StopTimeData> stopTimeData = transportDataReader.getStopTimes(new StopTimeDataMapper(includeAll));
            Stream<CalendarData> calendarData = transportDataReader.getCalendar(new CalendarDataMapper(includeAll));
            Stream<CalendarDateData> calendarsDates = transportDataReader.getCalendarDates(new CalendarDatesDataMapper(includeAll));
            Stream<AgencyData> agencyData = transportDataReader.getAgencies(new AgencyDataMapper(includeAll));

            TransportDataStreams transportDataStreams =
                    new TransportDataStreams(transportDataReader.getNameAndVersion(),
                            agencyData, stopData, routeData, tripData,
                            stopTimeData, calendarData, feedInfoData, calendarsDates, sourceConfig);
            dataStreams.add(transportDataStreams);
        });

        return new TransportDataFromFilesBuilderGeoFilter(dataStreams, stationLocations, config);
    }
}

