package com.tramchester.repository;

import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.Service;
import com.tramchester.domain.time.TramServiceDate;

import java.util.Set;

public interface ServiceRepository {
    Set<Service> getServices();
    Service getServiceById(IdFor<Service> serviceId);
    boolean hasServiceId(IdFor<Service> serviceId);

    Set<Service> getServicesOnDate(TramServiceDate date);


}
