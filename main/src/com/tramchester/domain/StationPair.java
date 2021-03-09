package com.tramchester.domain;

import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;

public class StationPair {
    private final IdFor<Station> begin;
    private final IdFor<Station> end;

    public StationPair(HasId<Station> begin, HasId<Station> end) {
        this.begin = begin.getId();
        this.end = end.getId();
    }

    public StationPair(IdFor<Station> begin, IdFor<Station> end) {
        this.begin = begin;
        this.end = end;
    }

    public static StationPair of(HasId<Station> begin, HasId<Station> end) {
        return new StationPair(begin, end);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StationPair that = (StationPair) o;

        if (!begin.equals(that.begin)) return false;
        return end.equals(that.end);
    }

    @Override
    public int hashCode() {
        int result = begin.hashCode();
        result = 31 * result + end.hashCode();
        return result;
    }

    public IdFor<Station> getBegin() {
        return begin;
    }

    public IdFor<Station> getEnd() {
        return end;
    }

    public boolean same() {
        return begin.equals(end);
    }
}
