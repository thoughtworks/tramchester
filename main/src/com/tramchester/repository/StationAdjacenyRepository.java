package com.tramchester.repository;

import com.tramchester.domain.Location;
import com.tramchester.domain.Station;
import com.tramchester.domain.TramTime;
import com.tramchester.domain.input.Stop;
import com.tramchester.domain.input.Stops;
import com.tramchester.domain.input.Trip;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class StationAdjacenyRepository {

    private final Map<String, Integer> matrix;
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
                String pair = formId(currentStop.getStation(), nextStop.getStation());
                if (!matrix.containsKey(pair)) {
                    int cost = TramTime.diffenceAsMinutes(currentStop.getDepartureTime(), nextStop.getArrivalTime());
                    matrix.put(pair, cost);
                }
            }
        });
    }

    private String formId(Location first, Location second) {
        return first.getId()+second.getId();
    }

    public int getAdjacent(Station firstStation, Station secondStation) {
        String id = formId(firstStation, secondStation);
        if (matrix.containsKey(id)) {
            return matrix.get(id);
        }
        return -1;
    }
}
