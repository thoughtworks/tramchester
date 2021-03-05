package com.tramchester.testSupport;

import com.tramchester.domain.id.HasId;
import com.tramchester.domain.places.Station;

public class StationPair {
    private final HasId<Station> begin;
    private final HasId<Station> end;

    public StationPair(HasId<Station> begin, HasId<Station> end) {
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

    public HasId<Station> getBegin() {
        return begin;
    }

    public HasId<Station> getEnd() {
        return end;
    }

    public boolean same() {
        return begin.getId().equals(end.getId());
    }
}
