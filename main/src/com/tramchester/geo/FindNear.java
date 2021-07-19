package com.tramchester.geo;

import com.tramchester.domain.places.Location;

import java.util.stream.Stream;

public class FindNear {

    public static <T extends Location<T>> Stream<T> getNearTo(Stream<T> positions, GridPosition otherPosition,
                                                               MarginInMeters rangeInMeters) {
        return FindNear.getNearbyCrude(positions, otherPosition, rangeInMeters).
                filter(entry -> GridPositions.withinDist(otherPosition, entry.getGridPosition(), rangeInMeters));
    }

    private static <T extends Location<T>> Stream<T> getNearbyCrude(
            Stream<T> positions,
            GridPosition otherPosition,
            MarginInMeters rangeInMeters) {

        return positions.filter(entry -> GridPositions.withinDistEasting(otherPosition, entry.getGridPosition(), rangeInMeters)).
                        filter(entry -> GridPositions.withinDistNorthing(otherPosition, entry.getGridPosition(), rangeInMeters));
    }

    public static <T extends Location<T>> Stream<T> getNearToSorted(Stream<T> positions,
                                                                    GridPosition gridPosition, MarginInMeters rangeInMeters) {
        return getNearTo(positions, gridPosition, rangeInMeters).
            sorted((a, b) -> compareDistancesNearestFirst(gridPosition, a.getGridPosition(), b.getGridPosition()));
    }

    public static int compareDistancesNearestFirst(GridPosition origin, GridPosition first, GridPosition second) {
        long firstDist = GridPositions.distanceTo(origin, first);
        long secondDist = GridPositions.distanceTo(origin, second);
        return Long.compare(firstDist, secondDist);
    }
}
