package com.tramchester.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tramchester.config.StationClosureConfig;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;

import java.time.LocalDate;


// config example
//      - station: 9400ZZMAEXS
//              begin: 2021-07-22
//              end: 2021-07-30

@JsonDeserialize(as=StationClosureConfig.class)
public interface StationClosure {

    IdFor<Station> getStation();
    LocalDate getBegin();
    LocalDate getEnd();

}
