package com.tramchester.repository;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.IdSet;
import com.tramchester.domain.Service;
import com.tramchester.domain.time.ServiceTime;
import com.tramchester.domain.time.TramServiceDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class RunningServices {
    private static final Logger logger = LoggerFactory.getLogger(RunningServices.class);

    private final IdSet<Service> serviceIds;
    private final Map<IdFor<Service>, ServiceTime> latestTimeMap;
    private final Map<IdFor<Service>, ServiceTime> earliestTimeMap;

    public RunningServices(TramServiceDate date, TransportData transportData, TramchesterConfig config) {
        serviceIds = new IdSet<>();
        latestTimeMap = new HashMap<>();
        earliestTimeMap = new HashMap<>();

        ServiceTime earliest = ServiceTime.of(0,0).plusMinutes(config.getMaxWait());

        transportData.getServicesOnDate(date).forEach(svc -> {
            IdFor<Service> serviceId = svc.getId();
            serviceIds.add(serviceId);
            latestTimeMap.put(serviceId, svc.latestDepartTime());
            ServiceTime earliestDepartTime = svc.earliestDepartTime();
            if (earliestDepartTime.isBefore(earliest)) {
                earliestDepartTime = earliest;
            }
            earliestTimeMap.put(serviceId, earliestDepartTime);
        });

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

    public ServiceTime getServiceLatest(IdFor<Service> svcId) {
        return latestTimeMap.get(svcId);
    }

    public ServiceTime getServiceEarliest(IdFor<Service> svcId) {
        return earliestTimeMap.get(svcId);
    }

    public long count() {
        return serviceIds.size();
    }
}
