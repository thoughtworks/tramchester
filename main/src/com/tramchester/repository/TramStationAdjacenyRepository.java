package com.tramchester.repository;

import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.input.StopCalls;
import com.tramchester.domain.input.Trip;
import org.apache.commons.lang3.tuple.Pair;
import org.picocontainer.Disposable;
import org.picocontainer.Startable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TramStationAdjacenyRepository implements Startable, Disposable {
    private static final Logger logger = LoggerFactory.getLogger(TramStationAdjacenyRepository.class);

    private final Map<Pair<Station,Station>, Integer> matrix;
    private final TransportData transportDataSource;

    public TramStationAdjacenyRepository(TransportData transportDataSource) {
        this.transportDataSource = transportDataSource;
        matrix = new HashMap<>();
    }

    @Override
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

    @Override
    public void stop() {

    }

    @Override
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
