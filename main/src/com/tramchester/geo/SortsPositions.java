package com.tramchester.geo;

import com.tramchester.domain.places.Station;
import com.tramchester.repository.StationRepository;

import java.util.*;
import java.util.stream.Collectors;

public class SortsPositions {
    private final StationRepository repository;

    public SortsPositions(StationRepository repository) {
        this.repository = repository;
    }

    private Double computeDistance(Station stationA, Station stationB) {
        return CoordinateTransforms.distanceInMiles(stationA.getLatLong(), stationB.getLatLong());
    }

    // List, order matters here
    public <CONTAINED> List<CONTAINED> sortedByNearTo(Set<String> destinationStationIds, Set<HasStationId<CONTAINED>> hasStationIds) {

        Set<Station> dests = destinationStationIds.stream().map(repository::getStation).collect(Collectors.toSet());

        Map<HasStationId<CONTAINED>, Double> distances = new HashMap<>();
        hasStationIds.forEach(container -> {
            double current = Double.MAX_VALUE;
            for (Station dest : dests) {
                Station place = repository.getStation(container.getStationId());
                double distance = computeDistance(place, dest);
                if (distance < current) {
                    current = distance;
                }
            }
            distances.put(container, current);
        });

        return distances.entrySet().stream().
                sorted(Comparator.comparingDouble(Map.Entry::getValue)).
                map(entry -> entry.getKey().getContained()).
                collect(Collectors.toList());

    }

    public interface HasStationId<T> {
        String getStationId();
        T getContained();
    }
}
