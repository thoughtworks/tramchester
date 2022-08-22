package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.CoreDomain;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdMap;
import com.tramchester.domain.time.CrossesDay;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.Duration;
import java.util.Set;

@LazySingleton
public class RunningRoutesAndServices {
    private static final Logger logger = LoggerFactory.getLogger(RunningRoutesAndServices.class);

    private final ServiceRepository serviceRepository;
    private final RouteRepository routeRepository;

    @Inject
    public RunningRoutesAndServices(ServiceRepository serviceRepository, RouteRepository routeRepository) {
        this.serviceRepository = serviceRepository;
        this.routeRepository = routeRepository;
    }

    public FilterForDate getFor(TramDate date) {
        IdMap<Service> serviceIds = getServicesFor(date);
        IdMap<Route> routeIds = getRoutesFor(date);

        TramDate nextDay = date.plusDays(1);
        IdMap<Service> runningServicesNextDay = getServicesFor(nextDay);
        IdMap<Route> runningRoutesNextDay = getRoutesFor(nextDay);

        TramDate previousDay = date.minusDays(1);
        IdMap<Service> previousDaySvcs = servicesIntoNextDay(previousDay);
        IdMap<Route> previousDayRoutes = routesIntoNextDayFor(previousDay);

        return new FilterForDate(serviceIds, routeIds, runningServicesNextDay, runningRoutesNextDay,
                previousDaySvcs, previousDayRoutes);
    }

    private IdMap<Route> routesIntoNextDayFor(TramDate date) {
        return intoNextDay(routeRepository.getRoutesRunningOn(date));
    }

    private IdMap<Service> servicesIntoNextDay(TramDate date) {
        return intoNextDay(serviceRepository.getServicesOnDate(date));
    }

    private <T extends HasId<T> & CoreDomain & CrossesDay> IdMap<T> intoNextDay(Set<T> items) {
        return items.stream().
                filter(CrossesDay::intoNextDay).
                collect(IdMap.collector());
    }

    @NotNull
    private IdMap<Route> getRoutesFor(TramDate date) {
        Set<Route> routes = routeRepository.getRoutesRunningOn(date);
        if (routes.size()>0) {
            logger.info("Found " + routes.size() + " running routes for " + date);
        } else
        {
            logger.warn("No running routes found on " + date);
        }
        return new IdMap<>(routes);
    }

    @NotNull
    private IdMap<Service> getServicesFor(TramDate date) {
        Set<Service> services = serviceRepository.getServicesOnDate(date);
        if (services.size()>0) {
            logger.info("Found " + services.size() + " running services for " + date);
        } else
        {
            logger.warn("No running services found on " + date);
        }
        return new IdMap<>(services);
    }

    public static class FilterForDate {
        private final IdMap<Service> servicesPreviousDay;
        private final IdMap<Route> routesPreviousDay;
        private final IdMap<Service> servicesToday;
        private final IdMap<Route> routesToday;
        private final IdMap<Service> servicesNextDay;
        private final IdMap<Route> routesNextDay;

        private FilterForDate(IdMap<Service> servicesToday, IdMap<Route> routesToday,
                              IdMap<Service> servicesNextDay, IdMap<Route> routesNextDay,
                              IdMap<Service> servicesPreviousDay, IdMap<Route> routesPreviousDay) {
            this.servicesToday = servicesToday;
            this.routesToday = routesToday;
            this.servicesNextDay = servicesNextDay;
            this.routesNextDay = routesNextDay;
            this.servicesPreviousDay = servicesPreviousDay;
            this.routesPreviousDay = routesPreviousDay;
        }

        public boolean isServiceRunningByDate(IdFor<Service> serviceId, boolean nextDay) {
            if (servicesToday.hasId(serviceId)) {
                return true;
            }

            if (nextDay) {
                return servicesNextDay.hasId(serviceId);
            } else {
                return servicesPreviousDay.hasId(serviceId);
            }
        }

        public boolean isRouteRunning(IdFor<Route> routeId, boolean nextDay) {
            if (routesToday.hasId(routeId)) {
                return true;
            }

            if (nextDay) {
                return routesNextDay.hasId(routeId);
            } else {
                return routesPreviousDay.hasId(routeId);
            }
        }

        @Override
        public String toString() {
            return "FilterForDate{" +
                    "servicesPreviousDay=" + HasId.asIds(servicesPreviousDay) +
                    ", routesPreviousDay=" + HasId.asIds(routesPreviousDay) +
                    ", servicesToday=" + HasId.asIds(servicesToday) +
                    ", routesToday=" + HasId.asIds(routesToday) +
                    ", servicesNextDay=" + HasId.asIds(servicesNextDay) +
                    ", routesNextDay=" + HasId.asIds(routesNextDay) +
                    '}';
        }

        public boolean isServiceRunningByTime(IdFor<Service> serviceId, TramTime time, int maxWait) {

            if (servicesToday.hasId(serviceId)) {
                Service todaySvc = servicesToday.get(serviceId);
                if (serviceOperatingWithin(todaySvc, time, maxWait)) {
                    return true;
                }
            }

            int hourOfDay = time.getHourOfDay();
            int minuteOfHour = time.getMinuteOfHour();

            if (time.isNextDay()) {
                if (servicesNextDay.hasId(serviceId)) {
                    // remove next day offset to get time for the following day
                    TramTime timeForNextDay = TramTime.of(hourOfDay, minuteOfHour);
                    Service nextDaySvc = servicesNextDay.get(serviceId);
                    return serviceOperatingWithin(nextDaySvc, timeForNextDay, maxWait);
                }
            } else {
                if (servicesPreviousDay.hasId(serviceId)) {
                    // use next day time, do any of previous days services run into today
                    TramTime timeForPreviousDay = TramTime.nextDay(hourOfDay, minuteOfHour);
                    Service previousDayService = servicesPreviousDay.get(serviceId);
                    return serviceOperatingWithin(previousDayService, timeForPreviousDay, maxWait);
                }
            }

            return false;
        }

        private boolean serviceOperatingWithin(Service service, TramTime time, int maxWait) {
            final TramTime finishTime = service.getFinishTime();
            final TramTime startTime = service.getStartTime();

            TimeRange withinService = TimeRange.of(startTime, finishTime);
            if (withinService.contains(time)) {
                return true;
            }

            // check if within wait time
            TimeRange range = TimeRange.of(startTime, Duration.ofMinutes(maxWait), Duration.ZERO);
            return range.contains(time);
        }
    }
}
