package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdMap;
import com.tramchester.domain.time.TramTime;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.LocalDate;
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

    public FilterForDate getFor(LocalDate date) {
        IdMap<Service> serviceIds = getServicesFor(date);
        IdMap<Route> routeIds = getRoutesFor(date);

        LocalDate nextDay = date.plusDays(1);
        IdMap<Service> runningServicesNextDay = getServicesFor(nextDay);
        IdMap<Route> runningRoutesNextDay = getRoutesFor(nextDay);

        return new FilterForDate(serviceIds, routeIds, runningServicesNextDay, runningRoutesNextDay);
    }

    @NotNull
    private IdMap<Route> getRoutesFor(LocalDate date) {
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
    private IdMap<Service> getServicesFor(LocalDate date) {
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
        private final IdMap<Service> runningServices;
        private final IdMap<Route> runningRoutes;
        private final IdMap<Service> runningServicesNextDay;
        private final IdMap<Route> runningRoutesNextDay;

        private FilterForDate(IdMap<Service> runningServices, IdMap<Route> runningRoutes,
                              IdMap<Service> runningServicesNextDay, IdMap<Route> runningRoutesNextDay) {
            this.runningServices = runningServices;
            this.runningRoutes = runningRoutes;
            this.runningServicesNextDay = runningServicesNextDay;
            this.runningRoutesNextDay = runningRoutesNextDay;
        }

        public boolean isServiceRunningByDate(IdFor<Service> serviceId, boolean nextDay) {
            if (nextDay && runningServicesNextDay.hasId(serviceId)) {
                return true;
            }
            return runningServices.hasId(serviceId);
        }

        public boolean isRouteRunning(IdFor<Route> routeId, boolean nextDay) {
            if (nextDay && runningRoutesNextDay.hasId(routeId)) {
                return true;
            }
            return runningRoutes.hasId(routeId);
        }

        @Override
        public String toString() {
            return "FilterForDate{" +
                    "number of runningServices=" + runningServices.size() +
                    ", number of runningRoutes=" + runningRoutes.size() +
                    '}';
        }

        public boolean isServiceRunningByTime(IdFor<Service> serviceId, TramTime time, int maxWait) {
            if (runningServices.hasId(serviceId)) {
                Service todaySvc = runningServices.get(serviceId);
                if (serviceOperatingWithin(todaySvc, time, maxWait)) {
                    return true;
                }
            }
            if (!time.isNextDay()) {
                return false;
            }

            // remove next day offset to get time for the following day
            TramTime timeForNextDay = TramTime.of(time.getHourOfDay(), time.getMinuteOfHour());
            if (runningServicesNextDay.hasId(serviceId)) {
                Service nextDaySvc = runningServicesNextDay.get(serviceId);
                return serviceOperatingWithin(nextDaySvc, timeForNextDay, maxWait);
            }
            return false;
        }

        private boolean serviceOperatingWithin(Service service, TramTime time, int maxWait) {
            final TramTime finishTime = service.getFinishTime();
            if (time.isAfter(finishTime)) {
                return false;
            }

            final TramTime startTime = service.getStartTime();
            if (time.between(startTime, finishTime)) {
                return true;
            }

            return time.withinInterval(maxWait, startTime);
        }
    }
}
