package com.tramchester.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.domain.StationClosure;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import io.dropwizard.Configuration;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.Set;

// config example
//      - stations: ["9400ZZMAEXS"]
//              begin: 2021-07-22
//              end: 2021-07-30

@Valid
public class StationClosureConfig extends Configuration implements StationClosure {

    private final Set<IdFor<Station>> stations;
    private final LocalDate begin;
    private final LocalDate end;

    public StationClosureConfig(@JsonProperty(value = "stations", required = true) Set<IdFor<Station>> stations,
                                @JsonProperty(value = "begin", required = true) LocalDate begin,
                                @JsonProperty(value = "end", required = true) LocalDate end) {
        this.stations = stations;
        this.begin = begin;
        this.end = end;
    }


    @Override
    public IdSet<Station> getStations() {
        return IdSet.wrap(stations);
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
                "stations=" + stations +
                ", begin=" + begin +
                ", end=" + end +
                "} ";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StationClosureConfig that = (StationClosureConfig) o;

        if (!stations.equals(that.stations)) return false;
        if (!begin.equals(that.begin)) return false;
        return end.equals(that.end);
    }

    @Override
    public int hashCode() {
        int result = stations.hashCode();
        result = 31 * result + begin.hashCode();
        result = 31 * result + end.hashCode();
        return result;
    }
}
