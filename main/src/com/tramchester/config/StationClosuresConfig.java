package com.tramchester.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.domain.StationClosures;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.dates.DateRange;
import io.dropwizard.Configuration;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

// config example
//    stationClosures:
//            - stations: [ "9400ZZMAECC", "9400ZZMALDY", "9400ZZMAWST" ]
//            begin: 2023-07-15
//            end: 2023-09-20
//            fullyClosed: true

@Valid
public class StationClosuresConfig extends Configuration implements StationClosures {

    private final Set<String> stationsText;
    private final LocalDate begin;
    private final LocalDate end;
    private final Boolean fullyClosed;

    // TODO Might need to change to Set<String> stations cand then convert afterwards
    public StationClosuresConfig(@JsonProperty(value = "stations", required = true) Set<String> stationsText,
                                 @JsonProperty(value = "begin", required = true) LocalDate begin,
                                 @JsonProperty(value = "end", required = true) LocalDate end,
                                 @JsonProperty(value = "fullyClosed", required = true) Boolean fullyClosed)  {
        this.stationsText = stationsText;
        this.begin = begin;
        this.end = end;
        this.fullyClosed = fullyClosed;
    }


    @Override
    public IdSet<Station> getStations() {
        //return IdSet.wrap(stationsText);
        return stationsText.stream().map(Station::createId).collect(IdSet.idCollector());
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
                "stations=" + stationsText +
                ", begin=" + begin +
                ", end=" + end +
                ", fullyClosed=" + fullyClosed +
                "} ";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StationClosuresConfig that = (StationClosuresConfig) o;
        return stationsText.equals(that.stationsText) && begin.equals(that.begin) && end.equals(that.end) && fullyClosed.equals(that.fullyClosed);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stationsText, begin, end, fullyClosed);
    }
}
