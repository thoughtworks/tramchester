package com.tramchester.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.StationClosure;
import com.tramchester.domain.places.Station;
import io.dropwizard.Configuration;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.Collections;

// config example
//      - station: 9400ZZMAEXS
//              begin: 2021-07-22
//              end: 2021-07-30

@Valid
public class StationClosureConfig extends Configuration implements StationClosure {

    private final StringIdFor<Station> station;
    private final LocalDate begin;
    private final LocalDate end;

    public StationClosureConfig(@JsonProperty(value = "station", required = true) StringIdFor<Station> station,
                                @JsonProperty(value = "begin", required = true) LocalDate begin,
                                @JsonProperty(value = "end", required = true) LocalDate end) {
        this.station = station;
        this.begin = begin;
        this.end = end;
    }


    @Override
    public IdSet<Station> getStations() {
        return IdSet.singleton(station);
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
                "} ";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StationClosureConfig that = (StationClosureConfig) o;

        if (!station.equals(that.station)) return false;
        if (!begin.equals(that.begin)) return false;
        return end.equals(that.end);
    }

    @Override
    public int hashCode() {
        int result = station.hashCode();
        result = 31 * result + begin.hashCode();
        result = 31 * result + end.hashCode();
        return result;
    }
}
