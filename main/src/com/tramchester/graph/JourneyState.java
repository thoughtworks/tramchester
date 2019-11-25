package com.tramchester.graph;

import com.tramchester.domain.TramTime;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.states.TraversalState;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.traversal.InitialBranchState;

import java.time.LocalTime;
import java.util.Objects;

public class JourneyState {
    // TODO Use tram time, more efficient
    private LocalTime journeyClock;
    private boolean onTram;

    private int journeyOffset;
    private LocalTime boardingTime;

    @Override
    public String toString() {
        return "JourneyState{" +
                "journeyClock=" + journeyClock +
                ", onTram=" + onTram +
                ", journeyOffset=" + journeyOffset +
                ", boardingTime=" + boardingTime +
                ", traversalState=" + traversalState +
                '}';
    }

    private TraversalState traversalState;

    public JourneyState(LocalTime queryTime, TraversalState traversalState) {
        this.journeyClock = queryTime;
        journeyOffset = 0;
        onTram = false;
        this.traversalState = traversalState;
    }

    public static JourneyState fromPrevious(JourneyState previousState) {
        return new JourneyState(previousState);
    }

    private JourneyState(JourneyState previousState) {
        this.journeyClock = previousState.journeyClock;
        this.onTram = previousState.onTram;
        this.journeyOffset = previousState.journeyOffset;
        this.traversalState = previousState.traversalState;
        if (onTram) {
            this.boardingTime = previousState.boardingTime;
        }
    }

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

    public TramTime getJourneyClock() {
        return TramTime.of(journeyClock);
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

    public JourneyState recordTramDetails(LocalTime boardingTime, int currentCost) throws TramchesterException {
        if (!onTram) {
            throw new TramchesterException("Not on a tram");
        }
        this.journeyClock = boardingTime;
        this.boardingTime = boardingTime;
        this.journeyOffset = currentCost;
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
        boardingTime = null;
        return this;
    }

    public boolean isOnTram() {
        return onTram;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JourneyState that = (JourneyState) o;
        return onTram == that.onTram &&
                Objects.equals(journeyClock, that.journeyClock);
    }

    @Override
    public int hashCode() {
        return Objects.hash(journeyClock, onTram);
    }
}
