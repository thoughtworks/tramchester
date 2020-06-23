package com.tramchester.geo;

import com.tramchester.domain.places.Station;
import com.tramchester.repository.StationRepository;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

public class SortsPositions {
    private final StationRepository repository;

    public SortsPositions(StationRepository repository) {
        this.repository = repository;
    }

    private Double computeDistance(Station stationA, Station stationB) {
        return CoordinateTransforms.distanceInMiles(stationA.getLatLong(), stationB.getLatLong());
    }

    // Destinations (when more then one) are stations clustered around a final walking destination
    // Is just choosing one of them a good enough approx? For most cases should be ok.
    public <T> List<T> sortedByNearTo(List<String> destinationStationIds, List<HasStationId<T>> relationships) {

        String firstStationId = destinationStationIds.get(0);
        Station dest = repository.getStation(firstStationId);

        Comparator<? super Pair<T, Double>> comparator = (Comparator<Pair<T, Double>>)
                (first, second) -> Double.compare(first.getRight(), second.getRight());
        TreeSet<Pair<T, Double>> sorted = new TreeSet<>(comparator);

        relationships.forEach(item -> sorted.add(Pair.of(item.getContained(),
                computeDistance(repository.getStation(item.getStationId()), dest))));

        ArrayList<T> results = new ArrayList<>(sorted.size());
        sorted.forEach(ordered -> results.add(ordered.getLeft()));
        return results;

    }

    public interface HasStationId<T> {
        String getStationId();
        T getContained();
    }
}
