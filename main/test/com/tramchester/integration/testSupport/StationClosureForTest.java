package com.tramchester.integration.testSupport;

import com.tramchester.domain.StationClosure;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.testSupport.reference.TramStations;

import java.time.LocalDate;

public class StationClosureForTest implements StationClosure {

    private final TramStations station;
    private final TramDate begin;
    private final TramDate end;

    public StationClosureForTest(TramStations station, TramDate begin, TramDate end) {
        this.station = station;
        this.begin = begin;
        this.end = end;
    }

    @Override
    public IdSet<Station> getStations() {
        return IdSet.singleton(station.getId());
    }

    @Override
    public TramDate getBegin() {
        return begin;
    }

    @Override
    public TramDate getEnd() {
        return end;
    }

    @Override
    public DateRange getDateRange() {
        return new DateRange(begin, end);
    }
}