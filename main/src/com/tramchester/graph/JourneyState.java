package com.tramchester.graph;

import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.states.TraversalState;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.traversal.InitialBranchState;

import java.time.LocalTime;

public class JourneyState {
    private LocalTime journeyClock;
    private boolean onTram;
    private int journeyOffset;

    private String tripId;
    private LocalTime boardingTime;

    @Override
    public String toString() {
        return "JourneyState{" +
                "journeyClock=" + journeyClock +
                ", onTram=" + onTram +
                ", journeyOffset=" + journeyOffset +
                ", tripId='" + tripId + '\'' +
                ", boardingTime=" + boardingTime +
                ", traversalState=" + traversalState +
                '}';
    }

    private TraversalState traversalState;

    public JourneyState(LocalTime queryTime, TraversalState traversalState) {
        this.journeyClock = queryTime;
        journeyOffset = 0;
        onTram = false;
        tripId = "";
        this.traversalState = traversalState;
    }

    public static JourneyState fromPrevious(JourneyState previousState) {
        return new JourneyState(previousState);
    }

    private JourneyState(JourneyState previousState) {
        this.journeyClock = previousState.journeyClock;
        this.tripId = previousState.tripId;
        this.onTram = previousState.onTram;
        this.journeyOffset = previousState.journeyOffset;
        this.traversalState = previousState.traversalState;
        if (onTram) {
            this.tripId = previousState.tripId;
            this.boardingTime = previousState.boardingTime;
        }
    }

//    public String getTripId() {
//        if (tripId.isEmpty()) {
//            throw new RuntimeException("Empty TripId");
//        }
//        return tripId;
//    }

    public static InitialBranchState<JourneyState> initialState(LocalTime queryTime,
                                                                TraversalState traversalState) {
        return new InitialBranchState<JourneyState>() {
            @Override
            public JourneyState initialState(Path path) { return new JourneyState(queryTime, traversalState);
            }

            @Override
            public InitialBranchState<JourneyState> reverse() {
                return null;
            }
        };
    }

    public LocalTime getJourneyClock() {
        return journeyClock;
    }

    public JourneyState updateJourneyClock(int currentTotalCost) {
        int costForTrip = currentTotalCost - journeyOffset;

        if (onTram) {
            journeyClock = boardingTime.plusMinutes(costForTrip);
        } else {
            journeyClock = journeyClock.plusMinutes(costForTrip);
        }
        return this;
    }

    public JourneyState recordTramDetails(LocalTime boardingTime, int currentCost, String tripId) throws TramchesterException {
        if (!onTram) {
            throw new TramchesterException("Not on a tram");
        }
        this.journeyClock = boardingTime;
        this.boardingTime = boardingTime;
        this.journeyOffset = currentCost;
        this.tripId = tripId;
        return this;
    }

    public JourneyState leaveTram(int currentTotalCost) throws TramchesterException {
        if (!onTram) {
            throw new TramchesterException("Not currently on a tram");
        }
        int tripCost = currentTotalCost - journeyOffset;
        journeyClock = boardingTime.plusMinutes(tripCost);

        journeyOffset = currentTotalCost;
        onTram = false;
        tripId = "";
        boardingTime = null;
        return this;
    }

    public boolean isOnTram() {
        return onTram;
    }

    public String getTripId() {
        return tripId;
    }

    public JourneyState boardTram() throws TramchesterException {
        if (isOnTram()) {
            throw new TramchesterException("Already on a tram");
        }
        onTram = true;
        return this;
    }

    public TraversalState getTraversalState() {
        return traversalState;
    }

    public JourneyState updateTraversalState(TraversalState traversalState) {
        this.traversalState = traversalState;
        return this;
    }
}
