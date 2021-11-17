package com.tramchester.dataimport.loader;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.TransportDataFactory;
import com.tramchester.dataimport.data.CalendarData;
import com.tramchester.dataimport.data.CalendarDateData;
import com.tramchester.domain.*;
import com.tramchester.domain.factory.TransportEntityFactory;
import com.tramchester.domain.id.CompositeIdMap;
import com.tramchester.domain.id.IdFor;
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
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static java.lang.String.format;

@LazySingleton
public class TransportDataFromFiles implements TransportDataFactory {
    private static final Logger logger = LoggerFactory.getLogger(TransportDataFromFiles.class);

    private final TransportDataStreams transportDataStreams;
    private final TramchesterConfig config;
    private final ProvidesNow providesNow;

    private final TransportDataContainer dataContainer;

    // NOTE: cannot inject GraphFilter here as circular dependency on being able to find routes which
    // needs transport data to be loaded....
    @Inject
    public TransportDataFromFiles(TransportDataStreams transportDataStreams,
                                  TramchesterConfig config, ProvidesNow providesNow) {
        this.transportDataStreams = transportDataStreams;
        this.config = config;
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
        transportDataStreams.forEach(transportDataStream -> load(transportDataStream, dataContainer));
        logger.info("started");
    }

    public TransportData getData() {
        return dataContainer;
    }

    private void load(TransportDataSource dataSource, WriteableTransportData buildable) {
        DataSourceInfo dataSourceInfo = dataSource.getDataSourceInfo();

        DataSourceID dataSourceInfoID = dataSourceInfo.getID();
        GTFSSourceConfig sourceConfig = dataSource.getConfig();

        logger.info("Loading data for " + dataSourceInfoID);

        if(sourceConfig.getHasFeedInfo()) {
            // replace version string (which is from mod time) with the one from the feedinfo file, if present
            Optional<FeedInfo> maybeFeedinfo = dataSource.getFeedInfoStream().findFirst();
            maybeFeedinfo.ifPresent(feedInfo -> {
                logger.info("Updating data source info from " + feedInfo);
                buildable.addDataSourceInfo(new DataSourceInfo(dataSourceInfoID, feedInfo.getVersion(),
                        dataSourceInfo.getLastModTime(), dataSource.getDataSourceInfo().getModes()));
                buildable.addFeedInfo(dataSourceInfoID, feedInfo);
            });

        } else {
            logger.warn("No feedinfo for " + dataSourceInfoID);
            buildable.addDataSourceInfo(dataSourceInfo);
        }

        TransportEntityFactory entityFactory = dataSource.getEntityFactory();
        StopDataLoader stopDataLoader = new StopDataLoader(entityFactory, config);
        AgencyDataLoader agencyDataLoader = new AgencyDataLoader(dataSourceInfoID, entityFactory);
        RouteDataLoader routeDataLoader = new RouteDataLoader(buildable, sourceConfig, entityFactory);
        TripLoader tripLoader = new TripLoader(buildable, entityFactory);
        StopTimeLoader stopTimeLoader = new StopTimeLoader(buildable, entityFactory, sourceConfig);

        PreloadedStationsAndPlatforms interimStations = stopDataLoader.load(dataSource.getStops());
        CompositeIdMap<Agency, MutableAgency> interimAgencies = agencyDataLoader.load(dataSource.getAgencies());
        RouteDataLoader.ExcludedRoutes excludedRoutes = routeDataLoader.load(dataSource.getRoutes(), interimAgencies);
        logger.info("Excluding " + excludedRoutes.numOfExcluded() + " routes ");
        interimAgencies.clear();

        TripAndServices interimTripsAndServices = tripLoader.load(dataSource.getTrips(), excludedRoutes);

        IdMap<Service> interimServices = stopTimeLoader.load(dataSource.getStopTimes(), interimStations, interimTripsAndServices);

        excludedRoutes.clear();
        interimStations.clear();

        populateCalendars(buildable, dataSource.getCalendars(), dataSource.getCalendarsDates(), interimServices, sourceConfig, entityFactory);

        interimTripsAndServices.clear();
        interimServices.clear();

        buildable.reportNumbers();

        // update svcs where calendar data is missing

        //buildable.getServices().stream().
        buildable.getServicesWithoutCalendar().
                forEach(svc -> logger.warn(format("source %s Service %s has missing calendar", dataSourceInfoID, svc.getId()))
        );

        reportZeroDaysServices(buildable);

        dataSource.closeAll();

        logger.info("Finishing Loading data for " + dataSourceInfoID);
    }

    private void reportZeroDaysServices(WriteableTransportData buildable) {
        IdSet<Service> noDayServices = buildable.getServicesWithZeroDays();
        if (!noDayServices.isEmpty()) {
            logger.warn("The following services do no operate on any days per calendar.txt file " + noDayServices);
        }
    }

    private void populateCalendars(WriteableTransportData buildable, Stream<CalendarData> calendars,
                                   Stream<CalendarDateData> calendarsDates, IdMap<Service> services, GTFSSourceConfig config,
                                   TransportEntityFactory factory) {
        AtomicInteger countCalendars = new AtomicInteger(0);
        logger.info("Loading calendars for " + services.size() +" services ");

        IdSet<Service> missingCalendar = services.getIds();
        calendars.forEach(calendarData -> {
            IdFor<Service> serviceId = calendarData.getServiceId();
            MutableService service = buildable.getServiceById(serviceId);

            if (service != null) {
                countCalendars.getAndIncrement();
                missingCalendar.remove(serviceId);
                ServiceCalendar serviceCalendar = factory.createServiceCalendar(calendarData);
                service.setCalendar(serviceCalendar);
            } else {
                // legit, we filter services based on the route transport mode
                logger.debug("Unable to find service " + serviceId + " while populating Calendar");
            }
        });

        if (!missingCalendar.isEmpty()) {
            logger.warn("Failed to match service id " + missingCalendar + " for calendar");
        }
        logger.info("Loaded " + countCalendars.get() + " calendar entries");

        updateServiceDatesFromCalendarDates(buildable, calendarsDates, services, config.getNoServices());
    }

    private void updateServiceDatesFromCalendarDates(WriteableTransportData buildable, Stream<CalendarDateData> calendarsDates,
                                                     IdMap<Service> services, Set<LocalDate> noServices) {
        logger.info("Loading calendar dates "+ services.size() +" services with no services on " + noServices);
        IdSet<Service> missingCalendarDates = services.getIds();
        AtomicInteger countCalendarDates = new AtomicInteger(0);

        calendarsDates.forEach(date -> {
            IdFor<Service> serviceId = date.getServiceId();
            MutableService service = buildable.getServiceById(serviceId);
            if (service != null) {
                if (service.hasCalendar()) {
                    countCalendarDates.getAndIncrement();
                    missingCalendarDates.remove(serviceId);
                    addException(date, service.getCalendar(), serviceId, noServices);
                } else {
                    // TODO Create a one off entry? Auto populate based on all exceptions? i.e. days of week
                    logger.error("Missing calendar for service " + service.getId() + " so could not add " + date);
                }
            } else  {
                // legit, we filter services based on the route transport mode
                logger.debug("Unable to find service " + serviceId + " while populating CalendarDates");
            }
        });
        if (!missingCalendarDates.isEmpty()) {
            logger.info("calendar_dates: Failed to find service id " + missingCalendarDates);
        }
        addNoServicesDatesToAllCalendars(services, noServices, missingCalendarDates);

        logger.info("Loaded " + countCalendarDates.get() + " calendar date entries");
    }

    private void addNoServicesDatesToAllCalendars(IdMap<Service> services, Set<LocalDate> noServices, IdSet<Service> missingCalendarDates) {
        if (noServices.isEmpty()) {
            return;
        }
        logger.warn("Adding no service dates from source config " + noServices);
        services.forEach(service -> {
            if (!missingCalendarDates.contains(service.getId())) {
                noServices.forEach(noServiceDate -> service.getCalendar().excludeDate(noServiceDate));
            }
        });
        logger.info("Added service dates");

    }

    private void addException(CalendarDateData date, ServiceCalendar calendar, IdFor<Service> serviceId, Set<LocalDate> noServices) {
        int exceptionType = date.getExceptionType();
        LocalDate exceptionDate = date.getDate();
        if (exceptionType == CalendarDateData.ADDED) {
            if (!noServices.contains(exceptionDate)) {
                calendar.includeExtraDate(exceptionDate);
            } else {
                logDateIssue(exceptionDate, noServices);
            }
        } else if (exceptionType == CalendarDateData.REMOVED) {
            calendar.excludeDate(exceptionDate);
        } else {
            logger.warn("Unexpected exception type " + exceptionType + " for service " + serviceId + " and date " + exceptionDate);
        }
    }

    private void logDateIssue(LocalDate exceptionDate, Set<LocalDate> noServices) {
        LocalDate currentDate = providesNow.getDate();
        String msg = format("Ignoring extra date %s as configured as no service date from %s", exceptionDate, noServices);
        if (currentDate.isAfter(exceptionDate)) {
            logger.debug(msg);
        } else {
            logger.warn(msg);
        }
    }
















}
