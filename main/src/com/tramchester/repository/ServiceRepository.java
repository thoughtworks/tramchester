package com.tramchester.repository;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.Service;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;

import java.time.LocalDate;
import java.util.Set;

@ImplementedBy(TransportData.class)
public interface ServiceRepository {
    Set<Service> getServices();
    Service getServiceById(IdFor<Service> serviceId);
    boolean hasServiceId(IdFor<Service> serviceId);

    Set<Service> getServicesOnDate(TramDate date);

}
