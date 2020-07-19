package com.tramchester.repository;

import com.tramchester.domain.time.ServiceTime;
import com.tramchester.domain.time.TramServiceDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RunningServices {
    private static final Logger logger = LoggerFactory.getLogger(RunningServices.class);

    private final Set<String> serviceIds;
    private final Map<String, ServiceTime> latestTimeMap;
    private final Map<String, ServiceTime> earliestTimeMap;

    public RunningServices(TramServiceDate date, TransportData transportData) {
        serviceIds = new HashSet<>();
        latestTimeMap = new HashMap<>();
        earliestTimeMap = new HashMap<>();

        transportData.getServicesOnDate(date).forEach(svc -> {
            String serviceId = svc.getId();
            serviceIds.add(serviceId);
            latestTimeMap.put(serviceId, svc.latestDepartTime());
            earliestTimeMap.put(serviceId, svc.earliestDepartTime());
        });

        if (serviceIds.size()>0) {
            logger.info("Found " + serviceIds.size() + " running services for " + date);
        } else
        {
            logger.warn("No running services found on " + date);
        }

    }

    public boolean isRunning(String serviceId) {
        return serviceIds.contains(serviceId);
    }

    public ServiceTime getServiceLatest(String svcId) {
        return latestTimeMap.get(svcId);
    }

    public ServiceTime getServiceEarliest(String svcId) {
        return earliestTimeMap.get(svcId);
    }

    public long count() {
        return serviceIds.size();
    }
}
