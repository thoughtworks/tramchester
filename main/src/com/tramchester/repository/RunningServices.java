package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.IdSet;
import com.tramchester.domain.Service;
import com.tramchester.domain.time.ServiceTime;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@LazySingleton
public class RunningServices {
    private static final Logger logger = LoggerFactory.getLogger(RunningServices.class);

    private final IdSet<Service> serviceIds;
    private final Map<IdFor<Service>, TramTime> latestTimeMap;
    private final Map<IdFor<Service>, TramTime> earliestTimeMap;

    public RunningServices(TramServiceDate date, TransportData transportData, TramchesterConfig config) {
        serviceIds = new IdSet<>();
        latestTimeMap = new HashMap<>();
        earliestTimeMap = new HashMap<>();

        TramTime earliest = ServiceTime.of(0,0).plusMinutes(config.getMaxWait());

        transportData.getServicesOnDate(date).forEach(svc -> {
            IdFor<Service> serviceId = svc.getId();
            serviceIds.add(serviceId);
            latestTimeMap.put(serviceId, svc.latestDepartTime());
            TramTime earliestDepartTime = svc.earliestDepartTime();
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

    public TramTime getServiceLatest(IdFor<Service> svcId) {
        return latestTimeMap.get(svcId);
    }

    public TramTime getServiceEarliest(IdFor<Service> svcId) {
        return earliestTimeMap.get(svcId);
    }

    public long count() {
        return serviceIds.size();
    }
}
