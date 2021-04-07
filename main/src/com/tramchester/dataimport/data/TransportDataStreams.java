package com.tramchester.dataimport.data;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.FetchDataFromUrlAndUnzip;
import com.tramchester.dataimport.TransportDataLoader;
import com.tramchester.dataimport.TransportDataReader;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.FeedInfo;
import com.tramchester.domain.factory.TransportEntityFactory;
import com.tramchester.domain.factory.TransportEntityFactoryForGBRail;
import com.tramchester.domain.factory.TransportEntityFactoryForTFGM;
import com.tramchester.repository.TransportDataSource;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

@LazySingleton
public class TransportDataStreams implements Iterable<TransportDataSource> {
    private static final Logger logger = LoggerFactory.getLogger(TransportDataStreams.class);

    private final List<TransportDataSource> theList;
    private final TransportDataLoader dataLoader;
    private final TramchesterConfig config;

    @Inject
    public TransportDataStreams(TransportDataLoader dataLoader, TramchesterConfig config,
                                FetchDataFromUrlAndUnzip.Ready dataIsDownloadedAndUnzipped) {
        this.dataLoader = dataLoader;
        this.config = config;
        theList = new ArrayList<>();
    }

    // Note: feedinfo is not mandatory in the standard
    @PostConstruct
    public void start() {
        logger.info("start");

        List<TransportDataReader> transportDataReaders = dataLoader.getReaders();

        // streams, so no data read yet

        transportDataReaders.forEach(transportDataReader -> {
            Stream<FeedInfo> feedInfoData = Stream.empty();
            GTFSSourceConfig sourceConfig = transportDataReader.getConfig();

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

            theList.add(transportDataSource);
        });

        logger.info("started");
    }

    private TransportEntityFactory getEntityFactoryFor(GTFSSourceConfig sourceConfig) {
        DataSourceID sourceName = new DataSourceID(sourceConfig.getName());
        if (DataSourceID.TFGM().equals(sourceName)) {
            return new TransportEntityFactoryForTFGM(config);
        } else if (DataSourceID.GBRail().equals(sourceName)) {
            return new TransportEntityFactoryForGBRail(config);
        } else {
            return new TransportEntityFactory(config);
        }
    }

    @NotNull
    @Override
    public Iterator<TransportDataSource> iterator() {
        return theList.iterator();
    }
}
