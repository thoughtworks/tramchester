package com.tramchester.domain.collections;


import java.util.Objects;

public class RouteIndexPair {
    private final short first;
    private final short second;
    private final int hashCode;

    private RouteIndexPair(short first, short second) {
        this.first = first;
        this.second = second;
        hashCode = Objects.hash(first, second);
    }

    static RouteIndexPair of(short first, short second) {
        return new RouteIndexPair(first, second);
    }

    @Deprecated
    public int firstAsInt() {
        return first;
    }

    @Deprecated
    public int secondAsInt() {
        return second;
    }

    public short first() {
        return first;
    }

    public short second() {
        return second;
    }

    public boolean isSame() {
        return first == second;
    }

    @Override
    public String toString() {
        return "RouteIndexPair{" +
                "first=" + first +
                ", second=" + second +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final RouteIndexPair routePair = (RouteIndexPair) o;
        return first == routePair.first && second == routePair.second;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }


}
