package com.tramchester.domain.input;

import com.tramchester.domain.places.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Stream;

public class StopCalls implements Iterable<StopCall> {
    private static final Logger logger = LoggerFactory.getLogger(StopCalls.class);

    private final ArrayList<StopCall> stops;

    // stationId -> [index into stops array]
    private final Map<String, List<Integer>> stations;

    public StopCalls() {
        stops = new ArrayList<>();
        stations = new HashMap<>();
    }

    public List<StopCall> getStopsFor(String stationId) {
        List<Integer> indexs = stations.get(stationId);

        List<StopCall> result = new LinkedList<>();
        indexs.forEach(index -> result.add(stops.get(index)));
        return result;
    }

    public void add(StopCall stop) {
        Location station = stop.getStation();
        if (station==null) {
            logger.error("Stop is missing station");
        } else {
            stops.add(stop);
            int index = stops.indexOf(stop);
            addStation(station.getId(), index);
        }
    }

    private void addStation(String stationId, int index) {
        if (!stations.containsKey(stationId)) {
            stations.put(stationId,new LinkedList<>());
        }
        stations.get(stationId).add(index);
    }

    public int size() {
        return stops.size();
    }

    public StopCall get(int index) {
        return stops.get(index);
    }

    public boolean callsAt(String stationId) {
        return stations.containsKey(stationId);
    }

    @Override
    public Iterator<StopCall> iterator() {
        return stops.iterator();
    }

    public Stream<StopCall> stream() {
        return stops.stream();
    }

    @Override
    public String toString() {
        return "Stops{" +
                "stops=" + stops +
                ", stations=" + stations +
                '}';
    }
}
