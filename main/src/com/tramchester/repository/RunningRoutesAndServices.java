package com.tramchester.repository;

import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.time.TramServiceDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunningRoutesAndServices {
    private static final Logger logger = LoggerFactory.getLogger(RunningRoutesAndServices.class);

    private final IdSet<Service> serviceIds;
    private final IdSet<Route> routeIds;

    public RunningRoutesAndServices(TramServiceDate date, ServiceRepository serviceRepository,
                                    RouteRepository routeRepository) {

        serviceIds = serviceRepository.getServicesOnDate(date).stream().collect(IdSet.collector());
        if (serviceIds.size()>0) {
            logger.info("Found " + serviceIds.size() + " running services for " + date);
        } else
        {
            logger.warn("No running services found on " + date);
        }

        routeIds = routeRepository.getRoutesRunningOn(date);
        if (routeIds.size()>0) {
            logger.info("Found " + routeIds.size() + " running routes for " + date);
        } else
        {
            logger.warn("No running routes found on " + date);
        }
    }

    // TODO next day?
    public boolean isRunning(IdFor<Service> serviceId) {
        return serviceIds.contains(serviceId);
    }

    // TODO next day?
    public boolean isRouteRunning(IdFor<Route> routeId) {
        return routeIds.contains(routeId);
    }

    @Override
    public String toString() {
        return "RunningServices{" +
                "serviceIds=" + serviceIds +
                '}';
    }

    public int size() {
        return serviceIds.size();
    }
}
