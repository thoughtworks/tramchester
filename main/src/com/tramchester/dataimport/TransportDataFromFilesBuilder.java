package com.tramchester.dataimport;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.DataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.data.*;
import com.tramchester.domain.FeedInfo;
import com.tramchester.domain.factory.TransportEntityFactory;
import com.tramchester.domain.factory.TransportEntityFactoryForGBRail;
import com.tramchester.domain.factory.TransportEntityFactoryForTFGM;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.repository.TransportDataFromFiles;
import com.tramchester.repository.TransportDataSource;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
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

        List<TransportDataSource> dataStreams = new ArrayList<>();

        transportDataReaders.forEach(transportDataReader -> {
            Stream<FeedInfo> feedInfoData = Stream.empty();
            DataSourceConfig sourceConfig = transportDataReader.getConfig();

            if (sourceConfig.getHasFeedInfo()) {
                feedInfoData = transportDataReader.getFeedInfo();
            }

            Stream<StopData> stopData = transportDataReader.getStops();
            Stream<RouteData> routeData = transportDataReader.getRoutes();
            Stream<TripData> tripData = transportDataReader.getTrips();
            Stream<StopTimeData> stopTimeData = transportDataReader.getStopTimes();
            Stream<CalendarData> calendarData = transportDataReader.getCalendar();
            Stream<CalendarDateData> calendarsDates = transportDataReader.getCalendarDates();
            Stream<AgencyData> agencyData = transportDataReader.getAgencies();

            TransportEntityFactory entityFactory = getEntityFactoryFor(sourceConfig);

            TransportDataSource transportDataSource =
                    new TransportDataSource(transportDataReader.getNameAndVersion(),
                            agencyData, stopData, routeData, tripData,
                            stopTimeData, calendarData, feedInfoData, calendarsDates, sourceConfig, entityFactory);

            dataStreams.add(transportDataSource);
        });

        return new TransportDataFromFiles(dataStreams, config, providesNow);
    }

    private TransportEntityFactory getEntityFactoryFor(DataSourceConfig sourceConfig) {
        String name = sourceConfig.getName();
        if ("tfgm".equals(name)) {
            return new TransportEntityFactoryForTFGM(config);
        } else if ("gb-rail".equals(name)) {
            return new TransportEntityFactoryForGBRail(config);
        } else {
            return new TransportEntityFactory(config);
        }
    }
}

