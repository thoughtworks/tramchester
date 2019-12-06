package com.tramchester.repository;

import com.tramchester.domain.Service;

import java.util.HashSet;
import java.util.Set;

public class RunningServices {
    private final Set<String> serviceIds;

    public RunningServices(Set<Service> services) {
        serviceIds = new HashSet<>();
        services.forEach(svc -> serviceIds.add(svc.getServiceId()));

    }

    public boolean isRunning(String serviceId) {
        return serviceIds.contains(serviceId);
    }

    public void addForTestingOnly(String svcId) {
        serviceIds.add(svcId);
    }
}
