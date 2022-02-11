package com.tramchester.geo;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.repository.StationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@LazySingleton
public class SortsPositions {
    private static final Logger logger = LoggerFactory.getLogger(SortsPositions.class);

    private final StationRepository repository;

    @Inject
    public SortsPositions(StationRepository repository) {
        this.repository = repository;
    }

    // List, order matters here
    public <CONTAINED> List<CONTAINED> sortedByNearTo(IdSet<Station> destinationStationIds, Set<HasStationId<CONTAINED>> startingPoints) {

        Set<Station> dests = destinationStationIds.stream().map(repository::getStationById).collect(Collectors.toSet());

        Map<HasStationId<CONTAINED>, Double> distances = new HashMap<>();
        startingPoints.forEach(container -> {
            double current = Double.MAX_VALUE;
            for (Station dest : dests) {
                Station place = repository.getStationById(container.getStationId());
                double distance = distanceFlat(place.getLatLong(), dest.getLatLong());
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
    public <CONTAINED> Stream<CONTAINED> sortedByNearTo(LatLong destination, Set<HasStationId<CONTAINED>> startingPoints) {

        Map<HasStationId<CONTAINED>, Double> distances = new HashMap<>();

        startingPoints.forEach(container -> {
                Station place = repository.getStationById(container.getStationId());
                double distance = distanceFlat(place.getLatLong(), destination);
                distances.put(container, distance);
        });

        return distances.entrySet().stream().
                sorted(Comparator.comparingDouble(Map.Entry::getValue)).
                map(entry -> entry.getKey().getContained());

    }

    public LatLong midPointFrom(LocationSet locations) {
        Set<LatLong> dests = locations.stream().map(Location::getLatLong).collect(Collectors.toSet());
        int size = dests.size();

        if (dests.isEmpty()) {
            logger.warn("No destination locations found for " + locations);
        }

        return dests.stream().
                reduce((A, B) -> new LatLong(A.getLat()+B.getLat(), A.getLon()+B.getLon())).
                map(latLong -> new LatLong(latLong.getLat()/ size, latLong.getLon()/ size)).
                orElse(LatLong.Invalid);
    }

    // crude but good enough for distance ranking during searches
    private double distanceFlat(LatLong point1, LatLong point2) {
        double deltaLat = Math.abs(point1.getLat()-point2.getLat());
        double deltaLon = Math.abs(point1.getLon()-point2.getLon());

        return Math.sqrt((deltaLat*deltaLat)+(deltaLon*deltaLon));
    }

    public interface HasStationId<T> {
        IdFor<Station> getStationId();
        T getContained();
    }
}
