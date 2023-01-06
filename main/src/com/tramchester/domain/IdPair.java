package com.tramchester.domain;

import com.tramchester.domain.id.IdFor;

import java.util.Objects;
import java.util.function.Function;

public class IdPair<T extends CoreDomain> {
    private final IdFor<T> first;
    private final IdFor<T> second;

    public IdPair(IdFor<T> first, IdFor<T> second) {
        this.first = first;
        this.second = second;
    }

    public boolean same() {
        return first.equals(second);
    }

    public IdFor<T> getFirst() {
        return first;
    }

    public IdFor<T> getSecond() {
        return second;
    }

    public boolean both(Function<IdFor<T>, Boolean> predicate) {
        return predicate.apply(first) && predicate.apply(second);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IdPair<?> idPair = (IdPair<?>) o;
        return first.equals(idPair.first) && second.equals(idPair.second);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second);
    }

    @Override
    public String toString() {
        return "IdPair{" +
                first +
                ", " + second +
                '}';
    }
}
