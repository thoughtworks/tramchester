package com.tramchester.domain;

import java.time.LocalDate;
import java.util.Objects;

public class RoutePair {
    private final Route first;
    private final Route second;

    public RoutePair(Route first, Route second) {
        this.first = first;
        this.second = second;
    }

    public static RoutePair of(Route routeA, Route routeB) {
        return new RoutePair(routeA, routeB);
    }

    public Route getFirst() {
        return first;
    }

    public Route getSecond() {
        return second;
    }

    @Override
    public String toString() {
        return "RoutePair{" +
                "first=" + first.getId() + " (" + first.getShortName() + ")" +
                ", second=" + second.getId() +" (" + second.getShortName() + ")" +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RoutePair routePair = (RoutePair) o;
        return first.equals(routePair.first) && second.equals(routePair.second);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second);
    }

    public boolean isAvailableOn(LocalDate date) {
        return first.isAvailableOn(date) && second.isAvailableOn(date);
    }

    public boolean areSame() {
        return first.equals(second);
    }

    public boolean sameMode() {
        return first.getTransportMode().equals(second.getTransportMode());
    }
}
