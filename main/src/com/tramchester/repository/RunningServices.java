package com.tramchester.repository;

import com.tramchester.domain.Service;
import com.tramchester.domain.time.ServiceTime;
import com.tramchester.domain.time.TramTime;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RunningServices {
    private final Set<String> serviceIds;
    private final Map<String, ServiceTime> latestTimeMap;
    private final Map<String, ServiceTime> earliestTimeMap;

    public RunningServices(Set<Service> services) {
        serviceIds = new HashSet<>();
        latestTimeMap = new HashMap<>();
        earliestTimeMap = new HashMap<>();

        services.forEach(svc -> {
            String serviceId = svc.getId();
            serviceIds.add(serviceId);
            latestTimeMap.put(serviceId, svc.latestDepartTime());
            earliestTimeMap.put(serviceId, svc.earliestDepartTime());
        });
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
