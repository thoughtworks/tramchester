package com.tramchester.domain.input;

import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Stream;

public class StopCalls {
    private static final Logger logger = LoggerFactory.getLogger(StopCalls.class);

    private final SortedMap<Integer, StopCall> stops;
    private final StringIdFor<Trip> parent;

    public StopCalls(StringIdFor<Trip> parent) {
        this.parent = parent;
        stops = new TreeMap<>();
    }

    public void dispose() {
        //logger.info("dispose for " + parent);
        stops.clear();
    }

    public void add(StopCall stop) {
        Station station = stop.getStation();
        if (station==null) {
            logger.error("Stop is missing station " + parent);
        } else {
            stops.put(stop.getGetSequenceNumber(), stop);
        }
    }

    public int numberOfCallingPoints() {
        return stops.size();
    }

    public StopCall getStopBySequenceNumber(int callingNumber) {
        return stops.get(callingNumber);
    }

    public boolean callsAt(HasId<Station> location) {
        return stops.values().stream().anyMatch(stopCall ->
                stopCall.getStationId().equals(location.getId()));
    }

    public Stream<StopCall> stream() {
        return stops.values().stream();
    }

    @Override
    public String toString() {
        return "Stops{" +
                "stops=" + stops +
                '}';
    }

    /**
     * Create StopLeg for each pair of stopcall (a,b,c,d,e) -> (a,b), (b,c), (c,d), (d,e)
     */
    public List<StopLeg> getLegs() {
        if (stops.isEmpty()) {
            logger.error("Missing stops, parent " + parent);
        }
        // Assume sorted map
        // TODO use stop sequence numbers instead?
        List<StopCall> calls = new ArrayList<>(stops.size());
        List<StopLeg> legs = new ArrayList<>(stops.size()-1);
        calls.addAll(stops.values());
        for (int i = 0; i < calls.size()-1; i++) {
            legs.add(new StopLeg(calls.get(i), calls.get(i+1)));
        }
        return legs;
    }

    public static class StopLeg {
        private final StopCall first;
        private final StopCall second;

        private StopLeg(StopCall first, StopCall second) {
            this.first = first;
            this.second = second;
        }

        public StopCall getFirst() {
            return first;
        }

        public StopCall getSecond() {
            return second;
        }

        @Override
        public String toString() {
            return "StopLeg{" +
                    "first=" + first +
                    ", second=" + second +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            StopLeg stopLeg = (StopLeg) o;

            if (!getFirst().equals(stopLeg.getFirst())) return false;
            return getSecond().equals(stopLeg.getSecond());
        }

        @Override
        public int hashCode() {
            int result = getFirst().hashCode();
            result = 31 * result + getSecond().hashCode();
            return result;
        }

        public Station getFirstStation() {
            return first.station;
        }

        public Station getSecondStation() {
            return second.station;
        }

        public TramTime getDepartureTime() {
            return first.getDepartureTime();
        }

        public int getCost() {
            return TramTime.diffenceAsMinutes(first.getDepartureTime(), second.getArrivalTime());
        }
    }
}
