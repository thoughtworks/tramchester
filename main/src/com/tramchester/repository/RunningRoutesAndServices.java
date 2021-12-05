package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.time.TramTime;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.time.LocalDate;

@LazySingleton
public class RunningRoutesAndServices {
    private static final Logger logger = LoggerFactory.getLogger(RunningRoutesAndServices.class);

    private final ServiceRepository serviceRepository;
    private final RouteRepository routeRepository;
    private final TripRepository tripRepository;
    private IdSet<Service> intoNextDay;

    @Inject
    public RunningRoutesAndServices(ServiceRepository serviceRepository, RouteRepository routeRepository, TripRepository tripRepository) {
        this.serviceRepository = serviceRepository;
        this.routeRepository = routeRepository;
        this.tripRepository = tripRepository;
    }

    @PostConstruct
    public void start() {
        logger.info("starting");
        intoNextDay = tripRepository.getTrips().stream().filter(Trip::intoNextDay).
                map(Trip::getService).
                collect(IdSet.collector());
        logger.info("started");
    }

    @PreDestroy
    public void dispose() {
        logger.info("stopping");
        if (intoNextDay != null) {
            intoNextDay.clear();
        }
        logger.info("stopped");
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

    public boolean intoNextDay(IdFor<Service> id) {
        return intoNextDay.contains(id);
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

        // TODO next day?
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
