package com.tramchester.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tramchester.config.StationClosureConfig;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.dates.DateRange;

import java.time.LocalDate;

@JsonDeserialize(as=StationClosureConfig.class)
public interface StationClosure {

    IdSet<Station> getStations();

    TramDate getBegin();
    TramDate getEnd();

    DateRange getDateRange();
}
