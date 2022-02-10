package com.tramchester.graph.search;

import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.search.stateMachine.states.TraversalState;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.traversal.InitialBranchState;

import java.time.Duration;
import java.util.LinkedList;
import java.util.List;

public class JourneyState implements ImmutableJourneyState, JourneyStateUpdate {

    private final CoreState coreState;

    private Duration journeyOffset;
    private TramTime boardingTime;
    private TraversalState traversalState;

    public JourneyState(TramTime queryTime, TraversalState traversalState) {
        coreState = new CoreState(queryTime);

        this.traversalState = traversalState;
        journeyOffset = Duration.ZERO;
    }

    public static JourneyState fromPrevious(ImmutableJourneyState previousState) {
        return new JourneyState((JourneyState) previousState);
    }

    // Copy cons
    private JourneyState(JourneyState previousState) {
        this.coreState = new CoreState(previousState.coreState);

        this.journeyOffset = previousState.journeyOffset;
        this.traversalState = previousState.traversalState;
        if (coreState.onBoard()) {
            this.boardingTime = previousState.boardingTime;
        }
    }

    public static InitialBranchState<JourneyState> initialState(TramTime queryTime, TraversalState traversalState) {
        return new InitialBranchState<>() {
            @Override
            public JourneyState initialState(Path path) {
                return new JourneyState(queryTime, traversalState);
            }

            @Override
            public InitialBranchState<JourneyState> reverse() {
                return null;
            }
        };
    }

    public TramTime getJourneyClock() {
        return coreState.journeyClock;
    }

    @Override
    public void updateTotalCost(Duration currentTotalCost) {
        Duration durationForTrip = currentTotalCost.minus(journeyOffset);

        if (coreState.onBoard()) {
            coreState.setJourneyClock(boardingTime.plus(durationForTrip));
        } else {
            coreState.incrementJourneyClock(durationForTrip);
        }
    }

    @Deprecated
    public void recordTime(TramTime boardingTime, int currentCost) throws TramchesterException {
        recordTime(boardingTime, Duration.ofMinutes(currentCost));
    }

    public void recordTime(TramTime boardingTime, Duration currentCost) throws TramchesterException {
        if ( !coreState.onBoard() ) {
            throw new TramchesterException("Not on a bus or tram");
        }
        coreState.setJourneyClock(boardingTime);
        this.boardingTime = boardingTime;
        this.journeyOffset = currentCost;
    }

    @Override
    public void beginWalk(Node beforeWalkNode, boolean atStart, Duration unused) {
        coreState.incrementWalkingConnections();
    }

    @Override
    public void beginTrip(IdFor<Trip> newTripId) {
        // noop
    }

    @Override
    public void endWalk(Node stationNode) {
        // noop
    }

    @Override
    public void toNeighbour(Node startNode, Node endNode, Duration cost) {
        coreState.incrementNeighbourConnections();
    }

    @Override
    public void seenStation(IdFor<Station> stationId) {
        coreState.seenStation(stationId);
    }

    @Override
    public void leave(TransportMode mode, Duration totalDuration, Node node) throws TramchesterException {
        if (!coreState.modeEquals(mode)) {
            throw new TramchesterException("Not currently on " +mode+ " was " + coreState.currentMode);
        }
        leave(totalDuration);
        coreState.leaveVehicle();
    }

    private void leave(Duration currentTotalCost) {
        if (currentTotalCost.compareTo(journeyOffset) < 0) {
            throw new RuntimeException("Invalid total cost "+currentTotalCost+" less that current total offset " +journeyOffset);
        }

        Duration tripCost = currentTotalCost.minus(journeyOffset); //currentTotalCost - journeyOffset;

        coreState.setJourneyClock(boardingTime.plus(tripCost));

        journeyOffset = currentTotalCost;
        boardingTime = null;
    }

    @Override
    public int getNumberChanges() {
        return coreState.getNumberOfChanges();
    }

    @Override
    public int getNumberWalkingConnections() {
        return coreState.numberOfWalkingConnections;
    }

    @Override
    public boolean hasBegunJourney() {
        return coreState.hasBegun;
    }

    @Override
    public int getNumberNeighbourConnections() {
        return coreState.numberNeighbourConnections;
    }

    @Override
    public void board(TransportMode mode, Node node, boolean hasPlatform) throws TramchesterException {
        guardAlreadyOnboard();
        coreState.board(mode);
    }

    private void guardAlreadyOnboard() throws TramchesterException {
        if (!coreState.currentMode.equals(TransportMode.NotSet)) {
            throw new TramchesterException("Already on a " + coreState.currentMode);
        }
    }

    public TraversalState getTraversalState() {
        return traversalState;
    }

    @Override
    public String getTraversalStateName() {
        return traversalState.getClass().getSimpleName();
    }

    @Deprecated
    @Override
    public int getTotalCostSoFar() {
        return (int) traversalState.getTotalCost().toMinutes();
    }

    @Override
    public Duration getTotalDurationSoFar() {
        return traversalState.getTotalDuration();
    }

    @Override
    public boolean hasVisited(IdFor<Station> stationId) {
        return coreState.visitedStations.contains(stationId);
    }

    public void updateTraversalState(TraversalState traversalState) {
        this.traversalState = traversalState;
    }


    @Override
    public TransportMode getTransportMode() {
        return coreState.currentMode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JourneyState that = (JourneyState) o;

        return coreState.equals(that.coreState);
    }

    @Override
    public int hashCode() {
        return coreState.hashCode();
    }

    @Override
    public String toString() {
        return "JourneyState{" +
                "coreState=" + coreState +
                ", journeyOffset=" + journeyOffset +
                ", boardingTime=" + boardingTime +
                ", traversalState=" + traversalState +
                '}';
    }

    private static class CoreState {
        private boolean hasBegun;
        private TramTime journeyClock;
        private TransportMode currentMode;
        private int numberOfBoardings;
        private int numberOfWalkingConnections;
        private int numberNeighbourConnections;
        private final List<IdFor<Station>> visitedStations;

        public CoreState(TramTime queryTime) {
            this(queryTime, false, 0,
                    TransportMode.NotSet, 0, 0, new LinkedList<>());
        }

        // Copy cons
        public CoreState(CoreState previous) {
            this(previous.journeyClock, previous.hasBegun, previous.numberOfBoardings, previous.currentMode, previous.numberOfWalkingConnections,
                    previous.numberNeighbourConnections, previous.visitedStations);
        }

        private CoreState(TramTime journeyClock, boolean hasBegun, int numberOfBoardings, TransportMode currentMode,
                          int numberOfWalkingConnections, int numberNeighbourConnections, List<IdFor<Station>> visitedStations) {
            this.hasBegun = hasBegun;
            this.journeyClock = journeyClock;
            this.currentMode = currentMode;
            this.numberOfBoardings = numberOfBoardings;
            this.numberOfWalkingConnections = numberOfWalkingConnections;
            this.numberNeighbourConnections = numberNeighbourConnections;
            this.visitedStations = visitedStations;
        }

        public void incrementWalkingConnections() {
            numberOfWalkingConnections = numberOfWalkingConnections + 1;
        }

        public void incrementNeighbourConnections() {
            numberNeighbourConnections = numberNeighbourConnections + 1;
        }

        public void board(TransportMode mode) {
            numberOfBoardings = numberOfBoardings + 1;
            currentMode = mode;
            hasBegun = true;
        }

        public void setJourneyClock(TramTime time) {
            journeyClock = time;
        }

        public void incrementJourneyClock(Duration duration) {
            journeyClock = journeyClock.plus(duration);
        }

        public boolean onBoard() {
            return !currentMode.equals(TransportMode.NotSet);
        }

        public void leaveVehicle() {
            currentMode = TransportMode.NotSet;
        }

        public int getNumberOfChanges() {
            if (numberOfBoardings==0) {
                return 0;
            }
            return numberOfBoardings-1; // initial boarding
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CoreState coreState = (CoreState) o;

            if (hasBegun != coreState.hasBegun) return false;
            if (numberOfBoardings != coreState.numberOfBoardings) return false;
            if (numberOfWalkingConnections != coreState.numberOfWalkingConnections) return false;
            if (numberNeighbourConnections != coreState.numberNeighbourConnections) return false;
            if (!journeyClock.equals(coreState.journeyClock)) return false;
            return currentMode == coreState.currentMode;
        }

        @Override
        public int hashCode() {
            int result = (hasBegun ? 1 : 0);
            result = 31 * result + journeyClock.hashCode();
            result = 31 * result + currentMode.hashCode();
            result = 31 * result + numberOfBoardings;
            result = 31 * result + numberOfWalkingConnections;
            result = 31 * result + numberNeighbourConnections;
            return result;
        }

        @Override
        public String toString() {
            return "CoreState{" +
                    "hasBegun=" + hasBegun +
                    ", journeyClock=" + journeyClock +
                    ", currentMode=" + currentMode +
                    ", numberOfBoardings=" + numberOfBoardings +
                    ", numberOfWalkingConnections=" + numberOfWalkingConnections +
                    ", numberNeighbourConnections=" + numberNeighbourConnections +
                    '}';
        }

        public boolean modeEquals(TransportMode mode) {
            return currentMode==mode;
        }

        public void seenStation(IdFor<Station> stationId) {
            visitedStations.add(stationId);
        }
    }

}
