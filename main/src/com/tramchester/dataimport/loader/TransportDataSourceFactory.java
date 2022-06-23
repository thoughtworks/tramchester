package com.tramchester.dataimport.loader;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.dataimport.UnzipFetchedData;
import com.tramchester.dataimport.data.*;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.FeedInfo;
import com.tramchester.domain.factory.TransportEntityFactory;
import com.tramchester.domain.factory.TransportEntityFactoryForTFGM;
import com.tramchester.repository.naptan.NaptanRepository;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

@LazySingleton
public class TransportDataSourceFactory implements Iterable<TransportDataSource> {
    private static final Logger logger = LoggerFactory.getLogger(TransportDataSourceFactory.class);

    private final List<TransportDataSource> theList;
    private final TransportDataReaderFactory readerFactory;
    private final NaptanRepository naptanRespository;

    @Inject
    public TransportDataSourceFactory(TransportDataReaderFactory readerFactory, NaptanRepository naptanRepository,
                                      UnzipFetchedData.Ready dataIsDownloadedAndUnzipped) {
        this.readerFactory = readerFactory;
        this.naptanRespository = naptanRepository;
        theList = new ArrayList<>();
    }


    public boolean hasDataSources() {
        return readerFactory.hasReaders();
    }

    // Note: feedinfo is not mandatory in the standard
    @PostConstruct
    public void start() {
        logger.info("start");

        List<TransportDataReader> transportDataReaders = readerFactory.getReaders();

        logger.info("Loading for " + transportDataReaders.size() + " readers ");

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

    @PreDestroy
    public void stop() {
        logger.info("Stopping");
        theList.forEach(TransportDataSource::closeAll);
        theList.clear();
        logger.info("Stopped");
    }

    private TransportEntityFactory getEntityFactoryFor(GTFSSourceConfig sourceConfig) {
        DataSourceID sourceID = DataSourceID.valueOf(sourceConfig.getName());
        if (DataSourceID.tfgm == sourceID) {
            return new TransportEntityFactoryForTFGM(naptanRespository);
        } else {
            throw new RuntimeException("No entity factory is defined for " + sourceConfig.getName());
        }
    }

    @NotNull
    @Override
    public Iterator<TransportDataSource> iterator() {
        return theList.iterator();
    }

}
