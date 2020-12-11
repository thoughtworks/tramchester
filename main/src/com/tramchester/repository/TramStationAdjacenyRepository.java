package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.input.StopCalls;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@LazySingleton
public class TramStationAdjacenyRepository  {
    private static final Logger logger = LoggerFactory.getLogger(TramStationAdjacenyRepository.class);

    private final Map<Pair<Station,Station>, Integer> matrix;
    private final TransportData transportDataSource;

    @Inject
    public TramStationAdjacenyRepository(TransportData transportDataSource) {
        this.transportDataSource = transportDataSource;
        matrix = new HashMap<>();
    }

    @PostConstruct
    public void start() {
        logger.info("Build adjacency matrix");
        Collection<Trip> trips = transportDataSource.getTrips();
        trips.stream().filter(TransportMode::isTram).forEach(trip -> {
            StopCalls stops = trip.getStops();
            stops.getLegs().forEach(leg -> {
                Pair<Station, Station> pair = Pair.of(leg.getFirstStation(), leg.getSecondStation());
                if (!matrix.containsKey(pair)) {
                    matrix.put(pair, leg.getCost());
                }
            });
        });
        logger.info("Finished building adjacency matrix");
    }

    @PreDestroy
    public void dispose() {
        matrix.clear();
    }

    //
    // Distance between two adjacent stations, or -1 if not next to each other
    //
    public int getAdjacent(Station firstStation, Station secondStation) {
        Pair<Station, Station> id = Pair.of(firstStation, secondStation);
        if (matrix.containsKey(id)) {
            return matrix.get(id);
        }
        return -1;
    }

    public Set<Pair<Station, Station>> getTramStationParis() {
        return matrix.keySet();
    }

}
