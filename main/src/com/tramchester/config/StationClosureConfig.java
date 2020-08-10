package com.tramchester.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.StationClosure;
import com.tramchester.domain.places.Station;
import io.dropwizard.Configuration;

import javax.validation.Valid;
import java.time.LocalDate;

@Valid
public class StationClosureConfig extends Configuration implements StationClosure {

    private final IdFor<Station> station;
    private final LocalDate begin;
    private final LocalDate end;

    public StationClosureConfig(@JsonProperty(value = "station", required = true) IdFor<Station> station,
                                @JsonProperty(value = "begin", required = true) LocalDate begin,
                                @JsonProperty(value = "end", required = true) LocalDate end) {
        this.station = station;
        this.begin = begin;
        this.end = end;
    }


    @Override
    public IdFor<Station> getStation() {
        return station;
    }

    @Override
    public LocalDate getBegin() {
        return begin;
    }

    @Override
    public LocalDate getEnd() {
        return end;
    }

    @Override
    public String toString() {
        return "StationClosureConfig{" +
                "station=" + station +
                ", begin=" + begin +
                ", end=" + end +
                "} " + super.toString();
    }
}
