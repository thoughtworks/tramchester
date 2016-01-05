package com.tramchester.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Stream;

public class Stops  implements Iterable<Stop> {
    private static final Logger logger = LoggerFactory.getLogger(Stops.class);

    private ArrayList<Stop> stops;
    private Map<String, List<Integer>> stations;

    public Stops() {
        stops = new ArrayList<>();
        stations = new HashMap<>();
    }

    public boolean visitsStation(String stationId) {
        return stations.containsKey(stationId);
    }

    public List<Stop> getStopsFor(String stationId) {
        List<Integer> indexs = stations.get(stationId);
        List<Stop> result = new LinkedList<>();
        indexs.forEach(index -> result.add(stops.get(index)));
        return result;
    }

    public void add(Stop stop) {
        Station station = stop.getStation();
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

    public boolean travelsBetween(String firstStationId, String lastStationId, int minutesFromMidnight) {
        if (!(stations.containsKey(firstStationId) && stations.containsKey(lastStationId))) {
            return false;
        }
        List<Integer[]> pairs = getPairs(firstStationId, lastStationId);

        // now see if any of the candidate journeys from stop to stop operate at the right time
        for(Integer[] pair : pairs) {
            Stop firstStop = stops.get(pair[0]);
            Stop secondStop = stops.get(pair[1]);
            if (secondStop.getArriveMinsFromMidnight()> firstStop.getDepartureMinFromMidnight()) {
                if (firstStop.getDepartureMinFromMidnight()>minutesFromMidnight) {
                    return true;
                }
            }
        }
        return false;

    }

    public List<Integer[]> getPairs(String firstStationId, String lastStationId) {
        // assemble possible pairs representing journeys from stop to stop
        List<Integer[]> pairs = new LinkedList<>();
        List<Integer> firstStationStops = stations.get(firstStationId);
        List<Integer> lastStationStops = stations.get(lastStationId);
        firstStationStops.forEach(firstStationStop -> lastStationStops.forEach(lastStationStop -> {
            if (lastStationStop>firstStationStop) {
                pairs.add(new Integer[] {firstStationStop,lastStationStop});
            }
        }));
        return pairs;
    }

    public int size() {
        return stops.size();
    }

    public Stop get(int index) {
        return stops.get(index);
    }

    public boolean callsAt(String stationId) {
        return stations.containsKey(stationId);
    }

    @Override
    public Iterator<Stop> iterator() {
        return stops.iterator();
    }

    public Stream<Stop> stream() {
        return stops.stream();
    }
}
