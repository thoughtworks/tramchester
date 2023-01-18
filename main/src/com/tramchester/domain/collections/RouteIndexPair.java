package com.tramchester.domain.collections;

public class RouteIndexPair {
    private final int first;
    private final int second;
    private final int hashCode;

    private RouteIndexPair(int first, int second) {
        this.first = first;
        this.second = second;
        hashCode = first *31 + second; // Objects.hash(first, second);
    }

    static RouteIndexPair of(int first, int second) {
        return new RouteIndexPair(first, second);
    }

    public int first() {
        return first;
    }

    public int second() {
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
        RouteIndexPair routePair = (RouteIndexPair) o;
        return first == routePair.first && second == routePair.second;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

}
