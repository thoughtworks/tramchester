package com.tramchester.geo;

import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.repository.StationRepository;

import java.util.*;
import java.util.stream.Collectors;

public class SortsPositions {
    private final StationRepository repository;

    public SortsPositions(StationRepository repository) {
        this.repository = repository;
    }

    private Double computeDistance(Station stationA, Station stationB) {
        return CoordinateTransforms.distanceFlat(stationA.getLatLong(), stationB.getLatLong());
    }

    // List, order matters here
    public <CONTAINED> List<CONTAINED> sortedByNearTo(Set<String> destinationStationIds, Set<HasStationId<CONTAINED>> startingPoints) {

        Set<Station> dests = destinationStationIds.stream().map(repository::getStation).collect(Collectors.toSet());

        Map<HasStationId<CONTAINED>, Double> distances = new HashMap<>();
        startingPoints.forEach(container -> {
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

    // List, order matters here
    public <CONTAINED> List<CONTAINED> sortedByNearTo(LatLong destination, Set<HasStationId<CONTAINED>> startingPoints) {

        Map<HasStationId<CONTAINED>, Double> distances = new HashMap<>();

        startingPoints.forEach(container -> {
                Station place = repository.getStation(container.getStationId());
                double distance = CoordinateTransforms.distanceFlat(place.getLatLong(), destination);
                distances.put(container, distance);
        });

        return distances.entrySet().stream().
                sorted(Comparator.comparingDouble(Map.Entry::getValue)).
                map(entry -> entry.getKey().getContained()).
                collect(Collectors.toList());

    }

    public LatLong midPointFrom(Set<String> destinationStationIds) {
        Set<LatLong> dests = destinationStationIds.stream().map(repository::getStation).map(Station::getLatLong).collect(Collectors.toSet());
        int size = dests.size();

        return dests.stream().
                reduce((A, B) -> new LatLong(A.getLat()+B.getLat(), A.getLon()+B.getLon())).
                map(latLong -> new LatLong(latLong.getLat()/ size, latLong.getLon()/ size)).get();
    }

    public interface HasStationId<T> {
        String getStationId();
        T getContained();
    }
}
