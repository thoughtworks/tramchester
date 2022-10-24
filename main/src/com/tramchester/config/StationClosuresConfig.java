package com.tramchester.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.domain.StationClosures;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.dates.DateRange;
import io.dropwizard.Configuration;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;

// config example
//      - stations: ["9400ZZMAEXS"]
//              begin: 2021-07-22
//              end: 2021-07-30

@Valid
public class StationClosuresConfig extends Configuration implements StationClosures {

    private final Set<IdFor<Station>> stations;
    private final LocalDate begin;
    private final LocalDate end;
    private final Boolean fullyClosed;

    public StationClosuresConfig(@JsonProperty(value = "stations", required = true) Set<IdFor<Station>> stations,
                                 @JsonProperty(value = "begin", required = true) LocalDate begin,
                                 @JsonProperty(value = "end", required = true) LocalDate end,
                                 @JsonProperty(value = "fullyClosed", required = true) Boolean fullyClosed)  {
        this.stations = stations;
        this.begin = begin;
        this.end = end;
        this.fullyClosed = fullyClosed;
    }


    @Override
    public IdSet<Station> getStations() {
        return IdSet.wrap(stations);
    }

    @Override
    public TramDate getBegin() {
        return TramDate.of(begin);
    }

    @Override
    public TramDate getEnd() {
        return TramDate.of(end);
    }

    @Override
    public boolean isFullyClosed() {
        return fullyClosed;
    }

    @JsonIgnore
    @Override
    public DateRange getDateRange() {
        return new DateRange(getBegin(), getEnd());
    }

    @Override
    public String toString() {
        return "StationClosureConfig{" +
                "stations=" + stations +
                ", begin=" + begin +
                ", end=" + end +
                ", fullyClosed=" + fullyClosed +
                "} " + super.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StationClosuresConfig that = (StationClosuresConfig) o;
        return stations.equals(that.stations) && begin.equals(that.begin) && end.equals(that.end) && fullyClosed.equals(that.fullyClosed);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stations, begin, end, fullyClosed);
    }
}
