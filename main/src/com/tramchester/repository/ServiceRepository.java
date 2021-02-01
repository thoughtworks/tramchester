package com.tramchester.repository;

import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.Service;
import com.tramchester.domain.time.TramServiceDate;

import java.util.Set;

public interface ServiceRepository {
    Set<Service> getServices();
    Service getServiceById(StringIdFor<Service> serviceId);
    boolean hasServiceId(StringIdFor<Service> serviceId);

    Set<Service> getServicesOnDate(TramServiceDate date);


}
