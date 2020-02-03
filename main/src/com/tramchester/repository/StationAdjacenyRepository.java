package com.tramchester.repository;

import com.tramchester.domain.Station;
import com.tramchester.domain.time.TramTime;
import com.tramchester.domain.input.Stop;
import com.tramchester.domain.input.Stops;
import com.tramchester.domain.input.Trip;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class StationAdjacenyRepository {

    private final Map<Pair<Station,Station>, Integer> matrix;
    private final TransportDataSource transportDataSource;

    public StationAdjacenyRepository(TransportDataSource transportDataSource) {
        this.transportDataSource = transportDataSource;
        matrix = new HashMap<>();

        buildAdjacencyMatrix();
    }

    private void buildAdjacencyMatrix() {
        Collection<Trip> trips = transportDataSource.getTrips();
        trips.forEach(trip -> {
            Stops stops = trip.getStops();
            for (int i = 0; i < stops.size() - 1; i++) {
                Stop currentStop = stops.get(i);
                Stop nextStop = stops.get(i + 1);
                Pair<Station, Station> pair = formId(currentStop.getStation(), nextStop.getStation());
                if (!matrix.containsKey(pair)) {
                    int cost = TramTime.diffenceAsMinutes(currentStop.getDepartureTime(), nextStop.getArrivalTime());
                    matrix.put(pair, cost);
                }
            }
        });
    }

    private Pair<Station,Station> formId(Station first, Station second) {
        return Pair.of(first,second);
    }

    public int getAdjacent(Station firstStation, Station secondStation) {
        Pair<Station, Station> id = formId(firstStation, secondStation);
        if (matrix.containsKey(id)) {
            return matrix.get(id);
        }
        return -1;
    }

    public Set<Pair<Station, Station>> getStationParis() {
        return matrix.keySet();
    }
}
