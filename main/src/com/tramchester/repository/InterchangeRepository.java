package com.tramchester.repository;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.places.InterchangeStation;
import com.tramchester.domain.places.Station;

import java.util.Set;

@ImplementedBy(Interchanges.class)
public interface InterchangeRepository {
    boolean isInterchange(Station station);

    Set<InterchangeStation> getAllInterchanges();

    int size();
}
