package com.tramchester.repository;

import com.tramchester.domain.Location;
import com.tramchester.domain.TimeWindow;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.presentation.ServiceTime;

import java.util.Optional;

public interface ServiceTimes {
    Trip getTrip(String tripId);
    Optional<ServiceTime> getFirstServiceTime(String serviceId, Location firstStation, Location lastStation,
                                                     TimeWindow window);
}
