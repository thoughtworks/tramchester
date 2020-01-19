package com.tramchester.repository;

import com.tramchester.domain.input.Trip;

public interface ServiceTimes {
    Trip getTrip(String tripId);
}
