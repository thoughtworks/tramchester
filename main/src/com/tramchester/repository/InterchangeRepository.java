package com.tramchester.repository;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.places.InterchangeStation;
import com.tramchester.domain.places.SimpleInterchangeStation;
import com.tramchester.domain.places.Location;

import java.util.Set;

@ImplementedBy(Interchanges.class)
public interface InterchangeRepository {
    boolean isInterchange(Location<?> location);
    Set<InterchangeStation> getAllInterchanges();
    int size();

    InterchangeStation getInterchange(Location<?> location);
}
