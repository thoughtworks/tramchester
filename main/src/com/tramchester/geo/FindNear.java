package com.tramchester.geo;

import com.tramchester.domain.places.Location;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class FindNear {

    public static <T extends Location<T>>  Stream<T> getNearTo(
            Set<T> positions,
            GridPosition otherPosition, long rangeInMeters) {
        return FindNear.getNearbyCrude(positions, otherPosition, rangeInMeters).
                filter(entry -> GridPositions.withinDist(otherPosition, entry.getGridPosition(), rangeInMeters));
    }

    private static <T extends Location<T>> Stream<T> getNearbyCrude(
            Set<T> positions,
            GridPosition otherPosition,
            long rangeInMeters) {

        return positions.stream().
                // crude filter initially
                        filter(entry -> GridPositions.withinDistEasting(otherPosition, entry.getGridPosition(), rangeInMeters)).
                        filter(entry -> GridPositions.withinDistNorthing(otherPosition, entry.getGridPosition(), rangeInMeters));
    }

    public static <T extends Location<T>> Stream<T> getNearToSorted(Set<T> positions,
                                                                    GridPosition gridPosition, long rangeInMeters) {
        return getNearTo(positions, gridPosition, rangeInMeters).
            sorted((a, b) -> compareDistances(gridPosition, a.getGridPosition(), b.getGridPosition()));
    }

    private static int compareDistances(GridPosition origin, GridPosition first, GridPosition second) {
        long firstDist = GridPositions.distanceTo(origin, first);
        long secondDist = GridPositions.distanceTo(origin, second);
        return Long.compare(firstDist, secondDist);
    }
}
