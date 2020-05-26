package com.tramchester.domain.input;

import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Stream;

public class StopCalls implements Iterable<StopCall> {
    private static final Logger logger = LoggerFactory.getLogger(StopCalls.class);

    private final ArrayList<StopCall> stops;

    public StopCalls() {
        stops = new ArrayList<>();
    }

    public void add(StopCall stop) {
        Station station = stop.getStation();
        if (station==null) {
            logger.error("Stop is missing station");
        } else {
            stops.add(stop);
        }
        stops.sort(Comparator.comparingInt(StopCall::getGetSequenceNumber));
    }

    public int size() {
        return stops.size();
    }

    public StopCall get(int index) {
        return stops.get(index);
    }

    public boolean callsAt(Location location) {
        return stops.stream().anyMatch(stopCall -> stopCall.getStation().getId().equals(location.getId()));
    }

    @Override
    public @NotNull Iterator<StopCall> iterator() {
        return stops.iterator();
    }

    public Stream<StopCall> stream() {
        return stops.stream();
    }

    @Override
    public String toString() {
        return "Stops{" +
                "stops=" + stops +
                '}';
    }
}
