package com.tramchester.repository;

import com.tramchester.domain.Service;
import com.tramchester.domain.time.TramServiceDate;

import java.util.Set;

public interface ServiceRepository {
    Set<Service> getServices();
    Service getServiceById(String serviceId);
    boolean hasServiceId(String serviceId);

    Set<Service> getServicesOnDate(TramServiceDate date);


}
