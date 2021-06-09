package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.StationPair;
import com.tramchester.domain.input.StopCalls;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.reference.TransportMode;
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

    // each station and it's direct neighbours

    private static final Logger logger = LoggerFactory.getLogger(TramStationAdjacenyRepository.class);

    private final Map<StationPair, Integer> matrix;
    private final TransportData transportData;

    @Inject
    public TramStationAdjacenyRepository(TransportData transportData) {
        this.transportData = transportData;
        matrix = new HashMap<>();
    }

    @PostConstruct
    public void start() {
        logger.info("Build adjacency matrix");
        Collection<Trip> trips = transportData.getTrips();
        trips.stream().filter(TransportMode::isTram).forEach(trip -> {
            StopCalls stops = trip.getStopCalls();
            stops.getLegs().forEach(leg -> {
                StationPair pair = StationPair.of(leg);
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
    public int getAdjacent(StationPair id) {
        //StationPair id = StationPair.of(firstStation, secondStation);
        if (matrix.containsKey(id)) {
            return matrix.get(id);
        }
        return -1;
    }

    public Set<StationPair> getTramStationParis() {
        return matrix.keySet();
    }

}
