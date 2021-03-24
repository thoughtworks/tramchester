package com.tramchester.repository;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.Service;
import com.tramchester.domain.time.TramServiceDate;

import java.util.Set;

@ImplementedBy(TransportData.class)
public interface ServiceRepository {
    Set<Service> getServices();
    Service getServiceById(IdFor<Service> serviceId);
    boolean hasServiceId(IdFor<Service> serviceId);

    Set<Service> getServicesOnDate(TramServiceDate date);


}
