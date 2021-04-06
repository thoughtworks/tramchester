package com.tramchester.repository;

import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.time.TramServiceDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunningServices {
    private static final Logger logger = LoggerFactory.getLogger(RunningServices.class);

    private final IdSet<Service> serviceIds;

    public RunningServices(TramServiceDate date, TransportData transportData) {

        serviceIds = transportData.getServicesOnDate(date).stream().collect(IdSet.collector());

        if (serviceIds.size()>0) {
            logger.info("Found " + serviceIds.size() + " running services for " + date);
        } else
        {
            logger.warn("No running services found on " + date);
        }
    }

    public boolean isRunning(IdFor<Service> serviceId) {
        return serviceIds.contains(serviceId);
    }

    public long count() {
        return serviceIds.size();
    }
}
