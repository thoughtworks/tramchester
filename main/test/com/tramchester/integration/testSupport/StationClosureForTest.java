package com.tramchester.integration.testSupport;

import com.tramchester.domain.StationClosure;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.testSupport.reference.TramStations;

import java.time.LocalDate;

public class StationClosureForTest implements StationClosure {

    private final TramStations station;
    private final LocalDate begin;
    private final LocalDate end;

    public StationClosureForTest(TramStations station, LocalDate begin, LocalDate end) {
        this.station = station;
        this.begin = begin;
        this.end = end;
    }

    @Override
    public IdSet<Station> getStations() {
        return IdSet.singleton(station.getId());
    }

    @Override
    public LocalDate getBegin() {
        return begin;
    }

    @Override
    public LocalDate getEnd() {
        return end;
    }
}