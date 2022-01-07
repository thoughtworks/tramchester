package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.time.TramTime;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.LocalDate;

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
        IdSet<Service> serviceIds = getServicesFor(date);
        IdSet<Route> routeIds = getRoutesFor(date);

        LocalDate nextDay = date.plusDays(1);
        IdSet<Service> runningServicesNextDay = getServicesFor(nextDay);
        IdSet<Route> runningRoutesNextDay = getRoutesFor(nextDay);

        return new FilterForDate(serviceIds, routeIds, runningServicesNextDay, runningRoutesNextDay);
    }

    @NotNull
    private IdSet<Route> getRoutesFor(LocalDate date) {
        IdSet<Route> routeIds = routeRepository.getRoutesRunningOn(date);
        if (routeIds.size()>0) {
            logger.info("Found " + routeIds.size() + " running routes for " + date);
        } else
        {
            logger.warn("No running routes found on " + date);
        }
        return routeIds;
    }

    @NotNull
    private IdSet<Service> getServicesFor(LocalDate date) {
        IdSet<Service> serviceIds =  serviceRepository.getServicesOnDate(date);
        if (serviceIds.size()>0) {
            logger.info("Found " + serviceIds.size() + " running services for " + date);
        } else
        {
            logger.warn("No running services found on " + date);
        }
        return serviceIds;
    }

    public static class FilterForDate {
        private final IdSet<Service> runningServices;
        private final IdSet<Route> runningRoutes;
        private final IdSet<Service> runningServicesNextDay;
        private final IdSet<Route> runningRoutesNextDay;

        private FilterForDate(IdSet<Service> runningServices, IdSet<Route> runningRoutes,
                              IdSet<Service> runningServicesNextDay, IdSet<Route> runningRoutesNextDay) {
            this.runningServices = runningServices;
            this.runningRoutes = runningRoutes;
            this.runningServicesNextDay = runningServicesNextDay;
            this.runningRoutesNextDay = runningRoutesNextDay;
        }

        public boolean isServiceRunning(IdFor<Service> serviceId, TramTime time) {
            if (time.isNextDay() && runningServicesNextDay.contains(serviceId)) {
                return true;
            }
            return runningServices.contains(serviceId);
        }

        public boolean isRouteRunning(IdFor<Route> routeId, TramTime time) {
            if (time.isNextDay() && runningRoutesNextDay.contains(routeId)) {
                return true;
            }
            return runningRoutes.contains(routeId);
        }

        @Override
        public String toString() {
            return "FilterForDate{" +
                    "number of runningServices=" + runningServices.size() +
                    ", number of runningRoutes=" + runningRoutes.size() +
                    '}';
        }
    }
}
