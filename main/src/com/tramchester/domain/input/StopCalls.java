package com.tramchester.domain.input;

import com.tramchester.domain.places.Location;
import com.tramchester.domain.time.TimeWindow;
import com.tramchester.domain.time.TramTime;
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

    public boolean visitsStation(String stationId) {
        return stations.containsKey(stationId);
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

    public boolean travelsBetween(String firstStationId, String lastStationId, TimeWindow window) {
        if (!(stations.containsKey(firstStationId) && stations.containsKey(lastStationId))) {
            // just means this set of stops doesn't cover these stations
            //logger.warn(format("No stops for %s to %s as one or more station missing", firstStationId, lastStationId));
            return false;
        }
        List<Integer[]> pairs = getPairsForTime(firstStationId, lastStationId, window);
        return !pairs.isEmpty();
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

    private boolean checkTiming(StopCall firstStop, StopCall secondStop, TimeWindow timeWindow) {
        TramTime firstStopDepartureTime = firstStop.getDepartureTime();
        TramTime secondStopArriveTime = secondStop.getArrivalTime();
        return TramTime.checkTimingOfStops(timeWindow, firstStopDepartureTime, secondStopArriveTime);
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
