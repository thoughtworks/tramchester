package com.tramchester.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.domain.StationClosure;
import com.tramchester.domain.places.Station;
import io.dropwizard.Configuration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Valid
@JsonIgnoreProperties(ignoreUnknown = false)
public class StationClosureConfig extends Configuration implements StationClosure {

    @NotNull
    @JsonProperty(value = "station")
    private Station station;

    @NotNull
    @JsonProperty(value = "begin")
    private LocalDate begin;

    @NotNull
    @JsonProperty(value = "end")
    private LocalDate end;

    @Override
    public Station getStation() {
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
