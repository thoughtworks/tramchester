package com.tramchester.domain.input;

import com.tramchester.domain.Location;
import com.tramchester.domain.TimeWindow;
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

    public boolean travelsBetween(String firstStationId, String lastStationId, TimeWindow window) {
        if (!(stations.containsKey(firstStationId) && stations.containsKey(lastStationId))) {
            //logger.warn(format("No stops for %s to %s as one or more station missing", firstStationId, lastStationId));
            return false;
        }
        List<Integer[]> pairs = getPairsForTime(firstStationId, lastStationId, window);
        return !pairs.isEmpty();
    }

    public List<BeginEnd> getBeginEndStopsFor(String firstStationId, String lastStationId,
                                              TimeWindow window) {
        List<BeginEnd> results = new LinkedList<>();
        List<Integer[]> pairs = getPairsForTime(firstStationId, lastStationId, window);
        for(Integer[] pair : pairs) {
            results.add(new BeginEnd(get(pair[0]), get(pair[1])));
        }
        return results;
    }

    private List<Integer[]> getPairsForTime(String firstStationId, String lastStationId, TimeWindow timeWindow) {
        // assemble possible pairs representing journeys from stop to stop
        List<Integer[]> pairs = new LinkedList<>();
        List<Integer> firstStationStops = stations.get(firstStationId);
        List<Integer> lastStationStops = stations.get(lastStationId);
        firstStationStops.forEach(firstStationStop -> lastStationStops.forEach(lastStationStop -> {
            if (lastStationStop>firstStationStop) {
                if (checkTiming(stops.get(firstStationStop), stops.get(lastStationStop), timeWindow)) {
                    pairs.add(new Integer[] {firstStationStop,lastStationStop});
                }
            }
        }));
        return pairs;
    }

    private boolean checkTiming(Stop firstStop, Stop secondStop, TimeWindow timeWindow) {
        return (secondStop.getArriveMinsFromMidnight()>=firstStop.getDepartureMinFromMidnight())
                && (firstStop.getDepartureMinFromMidnight() > timeWindow.minsFromMidnight())
                && ((firstStop.getDepartureMinFromMidnight()-timeWindow.minsFromMidnight()) <= timeWindow.withinMins());
        // does this need to be >= for buses??
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

    @Override
    public String toString() {
        return "Stops{" +
                "stops=" + stops +
                ", stations=" + stations +
                '}';
    }
}
