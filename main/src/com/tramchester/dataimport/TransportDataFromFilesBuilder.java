package com.tramchester.dataimport;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.DataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.data.*;
import com.tramchester.dataimport.parsers.*;
import com.tramchester.domain.FeedInfo;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.geo.StationLocations;
import com.tramchester.repository.TransportDataFromFiles;
import com.tramchester.repository.TransportDataSource;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.stream.Stream;

@LazySingleton
public class TransportDataFromFilesBuilder {

    private final List<TransportDataReader> transportDataReaders;
    private final ProvidesNow providesNow;
    private final TramchesterConfig config;

    @Inject
    public TransportDataFromFilesBuilder(TransportDataLoader providesLoader, ProvidesNow providesNow,
                                         TramchesterConfig config) {
        // TODO getReaders() into create()
        this.transportDataReaders = providesLoader.getReaders();
        this.providesNow = providesNow;
        this.config = config;
    }

    // Note: feedinfo is not mandatory in the standard
    public TransportDataFromFiles create() {
        // streams, so no data read yet

        Set<String> includeAll = Collections.emptySet();
        List<TransportDataSource> dataStreams = new ArrayList<>();

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

            TransportDataSource transportDataSource =
                    new TransportDataSource(transportDataReader.getNameAndVersion(),
                            agencyData, stopData, routeData, tripData,
                            stopTimeData, calendarData, feedInfoData, calendarsDates, sourceConfig);
            dataStreams.add(transportDataSource);
        });

        return new TransportDataFromFiles(dataStreams, config, providesNow);
    }
}

