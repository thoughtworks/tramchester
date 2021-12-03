package com.tramchester.dataimport.loader;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.factory.TransportEntityFactory;
import com.tramchester.domain.id.CompositeIdMap;
import com.tramchester.domain.id.IdMap;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.repository.TransportData;
import com.tramchester.repository.TransportDataContainer;
import com.tramchester.repository.WriteableTransportData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.Optional;

import static java.lang.String.format;

@LazySingleton
public class PopulateTransportDataFromSources implements TransportDataFactory {
    private static final Logger logger = LoggerFactory.getLogger(PopulateTransportDataFromSources.class);

    private final TransportDataSourceFactory transportDataSourceFactory;
    private final DirectDataSourceFactory directDataSourceFactory;
    private final TramchesterConfig tramchesterConfig;
    private final ProvidesNow providesNow;

    private final TransportDataContainer dataContainer;

    // NOTE: cannot inject GraphFilter here as circular dependency on being able to find routes which
    // needs transport data to be loaded....
    @Inject
    public PopulateTransportDataFromSources(TransportDataSourceFactory transportDataSourceFactory,
                                            DirectDataSourceFactory directDataSourceFactory,
                                            TramchesterConfig tramchesterConfig, ProvidesNow providesNow) {
        this.transportDataSourceFactory = transportDataSourceFactory;
        this.directDataSourceFactory = directDataSourceFactory;
        this.tramchesterConfig = tramchesterConfig;
        this.providesNow = providesNow;
        dataContainer = new TransportDataContainer(providesNow, "TransportDataFromFiles");
    }

    @PreDestroy
    public void stop() {
        logger.info("stopping");
        dataContainer.dispose();
        logger.info("stopped");
    }

    @PostConstruct
    public void start() {
        logger.info("start");
        if (transportDataSourceFactory.hasDataSources()) {
            logger.info("Load for gtfs sources");
            transportDataSourceFactory.forEach(transportDataSource -> load(transportDataSource, dataContainer));
        }
        logger.info("Load for direct sources");
        directDataSourceFactory.forEach(directDataSource -> {
            directDataSource.loadInto(dataContainer);
            dataContainer.addDataSourceInfo(directDataSource.getDataSourceInfo());
            dataContainer.reportNumbers();
        } );
        logger.info("started");
    }

    public TransportData getData() {
        return dataContainer;
    }

    private void load(TransportDataSource dataSource, WriteableTransportData writeableTransportData) {
        DataSourceInfo dataSourceInfo = dataSource.getDataSourceInfo();

        GTFSSourceConfig sourceConfig = dataSource.getConfig();

        logger.info("Loading data for " + dataSourceInfo);

        updateFeedInfoIfPresent(dataSource, writeableTransportData, dataSourceInfo, sourceConfig);

        TransportEntityFactory entityFactory = dataSource.getEntityFactory();

        // create loaders
        StopDataLoader stopDataLoader = new StopDataLoader(entityFactory, tramchesterConfig);
        AgencyDataLoader agencyDataLoader = new AgencyDataLoader(dataSourceInfo, entityFactory);
        RouteDataLoader routeDataLoader = new RouteDataLoader(writeableTransportData, sourceConfig, entityFactory);
        TripLoader tripLoader = new TripLoader(writeableTransportData, entityFactory);
        StopTimeLoader stopTimeLoader = new StopTimeLoader(writeableTransportData, entityFactory, sourceConfig);
        CalendarLoader calendarLoader = new CalendarLoader(writeableTransportData, entityFactory);
        CalendarDateLoader calendarDateLoader = new CalendarDateLoader(writeableTransportData, providesNow, sourceConfig);

        PreloadedStationsAndPlatforms interimStations = stopDataLoader.load(dataSource.getStops());
        CompositeIdMap<Agency, MutableAgency> interimAgencies = agencyDataLoader.load(dataSource.getAgencies());
        RouteDataLoader.ExcludedRoutes excludedRoutes = routeDataLoader.load(dataSource.getRoutes(), interimAgencies);

        interimAgencies.clear();

        TripAndServices interimTripsAndServices = tripLoader.load(dataSource.getTrips(), excludedRoutes);
        IdMap<Service> interimServices = stopTimeLoader.load(dataSource.getStopTimes(), interimStations, interimTripsAndServices);

        excludedRoutes.clear();
        interimStations.clear();

        calendarLoader.load(dataSource.getCalendars(), interimServices);
        calendarDateLoader.load(dataSource.getCalendarsDates(), interimServices);

        interimTripsAndServices.clear();
        interimServices.clear();

        writeableTransportData.reportNumbers();

        writeableTransportData.getServicesWithoutCalendar().
                forEach(svc -> logger.warn(format("source %s Service %s has missing calendar", dataSourceInfo, svc.getId()))
        );
        reportZeroDaysServices(writeableTransportData);

        dataSource.closeAll();

        logger.info("Finishing Loading data for " + dataSourceInfo);
    }

    private void updateFeedInfoIfPresent(TransportDataSource dataSource, WriteableTransportData writeableTransportData,
                                         DataSourceInfo dataSourceInfo, GTFSSourceConfig sourceConfig) {
        if(sourceConfig.getHasFeedInfo()) {
            DataSourceID dataSourceInfoID = dataSourceInfo.getID();

            // replace version string (which is from mod time) with the one from the feedinfo file, if present
            Optional<FeedInfo> maybeFeedinfo = dataSource.getFeedInfoStream().findFirst();
            maybeFeedinfo.ifPresent(feedInfo -> {
                logger.info("Updating data source info from " + feedInfo);
                writeableTransportData.addDataSourceInfo(new DataSourceInfo(dataSourceInfoID, feedInfo.getVersion(),
                        dataSourceInfo.getLastModTime(), dataSource.getDataSourceInfo().getModes()));
                writeableTransportData.addFeedInfo(dataSourceInfoID, feedInfo);
            });

        } else {
            logger.warn("No feedinfo for " + dataSourceInfo);
            writeableTransportData.addDataSourceInfo(dataSourceInfo);
        }
    }

    private void reportZeroDaysServices(WriteableTransportData buildable) {
        IdSet<Service> noDayServices = buildable.getServicesWithZeroDays();
        if (!noDayServices.isEmpty()) {
            logger.warn("The following services do no operate on any days per calendar.txt file " + noDayServices);
        }
    }


















}
