package com.tramchester.domain.input;

import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.domain.reference.GTFSPickupDropoffType.None;
import static java.lang.String.format;

public class StopCalls {
    private static final Logger logger = LoggerFactory.getLogger(StopCalls.class);

    private final SortedMap<Integer, StopCall> orderedStopCalls;
    private final IdFor<Trip> parent;
    private boolean intoNextDay;

    public StopCalls(IdFor<Trip> parent) {
        this.parent = parent;
        orderedStopCalls = new TreeMap<>();
        intoNextDay = false;
    }

    public void dispose() {
        orderedStopCalls.clear();
    }

    public void add(StopCall stopCall) {
        Station station = stopCall.getStation();
        if (station==null) {
            logger.error("Stop is missing station " + parent);
            return;
        }

        final int sequenceNumber = stopCall.getGetSequenceNumber();
        if (orderedStopCalls.containsKey(sequenceNumber)) {
            // this can happen as duplicated stop calls occur in tfgm data occasionally
            if (!stopCall.same(orderedStopCalls.get(sequenceNumber))) {
                logger.error(format("Different stop already present for trip %s, already had %s inserting %s ",
                        parent, orderedStopCalls.get(sequenceNumber), stopCall));
            } else {
                logger.debug("Duplicated stopcall " + stopCall);
            }
        }
        orderedStopCalls.put(sequenceNumber, stopCall);
        intoNextDay = intoNextDay || stopCall.intoNextDay();

    }

    public long numberOfCallingPoints() {
        return orderedStopCalls.values().stream().filter(StopCall::callsAtStation).count();
    }

    public StopCall getStopBySequenceNumber(int callingNumber) {
        return orderedStopCalls.get(callingNumber);
    }

    public boolean callsAt(HasId<Station> stationId) {
        return orderedStopCalls.values().stream().anyMatch(stopCall ->
                stopCall.getStationId().equals(stationId.getId()));
    }

    public Stream<StopCall> stream() {
        return orderedStopCalls.values().stream();
    }

    @Override
    public String toString() {
        return "StopCalls{" +
                "stops=" + orderedStopCalls +
                '}';
    }

    /**
     * Create StopLeg for each pair of stopcall (a,b,c,d,e) -> (a,b), (b,c), (c,d), (d,e)
     * Respects the dropoff and pickup types so so skips stopcalls that just pass a station
     * @param filtered
     */
    public List<StopLeg> getLegs(boolean filtered) {
        if (orderedStopCalls.isEmpty()) {
            String msg = "Missing stops, parent trip " + parent;
            logger.error(msg);
            throw new RuntimeException(msg);
        }

        // Assume sorted map

        List<StopLeg> legs = new ArrayList<>();
        Iterator<StopCall> stopsIter = orderedStopCalls.values().iterator();
        StopCall next = null;
        while (stopsIter.hasNext()) {
            StopCall first = findNextPickup(stopsIter, next);
            StopCall second = findNextDropoff(stopsIter);
            if (first!=null && second!=null) {
                StopLeg stopLeg = new StopLeg(first, second);
                legs.add(stopLeg);
            }
            next = second;
        }
        if (legs.isEmpty() && !filtered) {
            logger.warn("No stop legs generated for " + this);
        }
        return legs;
    }

    private StopCall findNextPickup(Iterator<StopCall> iter, StopCall next) {
        if (next!=null) {
            if (next.getPickupType().isPickup()) {
                return next;
            }
        }
        while (iter.hasNext()) {
            StopCall current = iter.next();
            if (current.getPickupType().isPickup()) {
                return current;
            }
        }
        return null;
    }

    private StopCall findNextDropoff(Iterator<StopCall> iter) {
        while (iter.hasNext()) {
            StopCall current = iter.next();
            if (current.getDropoffType()!=None) {
                return current;
            }
        }
        return null;
    }

    public boolean intoNextDay() {
        return intoNextDay;
    }

    public List<Station> getStationSequence() {
        return orderedStopCalls.values().stream().map(StopCall::getStation).collect(Collectors.toList());
    }

    public StopCall getFirstStop() {
        int firstKey = orderedStopCalls.firstKey();
        return orderedStopCalls.get(firstKey);
    }

    public StopCall getLastStop() {
        int lastKey = orderedStopCalls.lastKey();
        return orderedStopCalls.get(lastKey);
    }

    public long totalNumber() {
        return orderedStopCalls.size();
    }

    public boolean isEmpty() {
        return orderedStopCalls.isEmpty();
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
