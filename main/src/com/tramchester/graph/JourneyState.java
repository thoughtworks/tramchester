package com.tramchester.graph;

import com.tramchester.domain.TramTime;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.traversal.InitialBranchState;

import java.time.LocalTime;

public class JourneyState {
    private TramTime currentTime;
    private final String tripId;

    public JourneyState(LocalTime queryTime) {
        this(queryTime, "");
    }

    public JourneyState(LocalTime queryTime, String tripId) {
        this.currentTime = TramTime.of(queryTime);
        this.tripId = tripId;
    }

    public static InitialBranchState<JourneyState> initialState(LocalTime queryTime) {
        return new InitialBranchState<JourneyState>() {
            @Override
            public JourneyState initialState(Path path) {
                return new JourneyState(queryTime);
            }

            @Override
            public InitialBranchState<JourneyState> reverse() {
                return null;
            }
        };
    }

    public LocalTime getTime() {
        return currentTime.asLocalTime();
    }

    public String getTripId() {
        if (tripId.isEmpty()) {
            throw new RuntimeException("Empty TripId");
        }
        return tripId;
    }

    public boolean hasIdTrip() {
        return !tripId.isEmpty();
    }
}
