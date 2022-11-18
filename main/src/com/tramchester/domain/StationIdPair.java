package com.tramchester.domain;

import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;

import java.util.Objects;

public class StationIdPair {

    private final IdPair<Station> pair;

    public StationIdPair(HasId<Station> begin, HasId<Station> end) {
        this(begin.getId(), end.getId());
    }

    public StationIdPair(IdFor<Station> begin, IdFor<Station> end) {
        pair = new IdPair<>(begin, end);
    }

    public static StationIdPair of(HasId<Station> begin, HasId<Station> end) {
        return new StationIdPair(begin, end);
    }

    public static StationIdPair of(IdFor<Station> begin, IdFor<Station> end) {
        return new StationIdPair(begin, end);
    }

    public IdFor<Station> getBeginId() {
        return pair.getFirst();
    }

    public IdFor<Station> getEndId() {
        return pair.getSecond();
    }

    public boolean same() {
        return pair.same();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StationIdPair that = (StationIdPair) o;
        return pair.equals(that.pair);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pair);
    }

    @Override
    public String toString() {
        return "StationIdPair{" +
                pair.getFirst() +
                ", " + pair.getSecond() +
                '}';
    }
}
