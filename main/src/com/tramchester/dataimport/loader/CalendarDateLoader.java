package com.tramchester.dataimport.loader;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.dataimport.data.CalendarDateData;
import com.tramchester.domain.MutableService;
import com.tramchester.domain.Service;
import com.tramchester.domain.dates.MutableServiceCalendar;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdMap;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.repository.WriteableTransportData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static java.lang.String.format;

public class CalendarDateLoader {
    private static final Logger logger = LoggerFactory.getLogger(CalendarDateLoader.class);

    private final WriteableTransportData buildable;
    private final ProvidesNow providesNow;
    private final GTFSSourceConfig gtfsSourceConfig;

    public CalendarDateLoader(WriteableTransportData buildable, ProvidesNow providesNow, GTFSSourceConfig gtfsSourceConfig) {
        this.buildable = buildable;
        this.providesNow = providesNow;
        this.gtfsSourceConfig = gtfsSourceConfig;
    }

    public void load(Stream<CalendarDateData> calendarsDates, IdMap<Service> services) {
        Set<LocalDate> noServices = gtfsSourceConfig.getNoServices();
        logger.info("Loading calendar dates "+ services.size() +" services with no services on " + noServices);
        IdSet<Service> missingCalendarDates = services.getIds();
        AtomicInteger countCalendarDates = new AtomicInteger(0);

        calendarsDates.forEach(date -> {
            IdFor<Service> serviceId = date.getServiceId();
            MutableService service = buildable.getMutableService(serviceId);
            if (service != null) {
                if (service.hasCalendar()) {
                    countCalendarDates.getAndIncrement();
                    missingCalendarDates.remove(serviceId);
                    addException(date, service.getMutableCalendar(), serviceId, noServices);
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
                noServices.forEach(noServiceDate -> {
                    MutableService mutableService = buildable.getMutableService(service.getId());
                    mutableService.getMutableCalendar().excludeDate(noServiceDate);
                });
            }
        });
        logger.info("Added service dates");

    }

    private void addException(CalendarDateData date, MutableServiceCalendar calendar, IdFor<Service> serviceId, Set<LocalDate> noServices) {
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
