package com.tramchester.geo;

import com.tramchester.domain.IdFor;
import com.tramchester.domain.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.repository.StationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class SortsPositions {
    private static final Logger logger = LoggerFactory.getLogger(SortsPositions.class);

    private final StationRepository repository;

    public SortsPositions(StationRepository repository) {
        this.repository = repository;
    }

    private Double computeDistance(Station stationA, Station stationB) {
        return CoordinateTransforms.distanceFlat(stationA.getLatLong(), stationB.getLatLong());
    }

    // List, order matters here
    public <CONTAINED> List<CONTAINED> sortedByNearTo(IdSet<Station> destinationStationIds, Set<HasStationId<CONTAINED>> startingPoints) {

        Set<Station> dests = destinationStationIds.stream().map(repository::getStationById).collect(Collectors.toSet());

        Map<HasStationId<CONTAINED>, Double> distances = new HashMap<>();
        startingPoints.forEach(container -> {
            double current = Double.MAX_VALUE;
            for (Station dest : dests) {
                Station place = repository.getStationById(container.getStationId());
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
                Station place = repository.getStationById(container.getStationId());
                double distance = CoordinateTransforms.distanceFlat(place.getLatLong(), destination);
                distances.put(container, distance);
        });

        return distances.entrySet().stream().
                sorted(Comparator.comparingDouble(Map.Entry::getValue)).
                map(entry -> entry.getKey().getContained()).
                collect(Collectors.toList());

    }

    public LatLong midPointFrom(Set<Station> destinationStation) {
        Set<LatLong> dests = destinationStation.stream().map(Station::getLatLong).collect(Collectors.toSet());
        int size = dests.size();

        if (dests.isEmpty()) {
            logger.warn("No destinations");
        }

        return dests.stream().
                reduce((A, B) -> new LatLong(A.getLat()+B.getLat(), A.getLon()+B.getLon())).
                map(latLong -> new LatLong(latLong.getLat()/ size, latLong.getLon()/ size)).get();
    }

    public interface HasStationId<T> {
        IdFor<Station> getStationId();
        T getContained();
    }
}
