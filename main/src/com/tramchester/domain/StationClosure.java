package com.tramchester.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tramchester.config.StationClosureConfig;
import com.tramchester.domain.places.Station;

import java.time.LocalDate;

@JsonDeserialize(as=StationClosureConfig.class)
public interface StationClosure {

    IdFor<Station> getStation();
    LocalDate getBegin();
    LocalDate getEnd();

}
