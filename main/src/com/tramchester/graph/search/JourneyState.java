package com.tramchester.graph.search;

import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.search.stateMachine.states.TraversalState;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.traversal.InitialBranchState;

import java.util.Objects;

public class JourneyState implements ImmutableJourneyState, JourneyStateUpdate {
    private TramTime journeyClock;
    private TransportMode transportMode;

    private int journeyOffset;
    private TramTime boardingTime;
    private TraversalState traversalState;
    private int numberOfBoardings;
    private int numberOfWalkingConnections;
    private boolean hasBegun;

    public JourneyState(TramTime queryTime, TraversalState traversalState) {
        this.journeyClock = queryTime;
        this.traversalState = traversalState;

        journeyOffset = 0;
        transportMode = TransportMode.NotSet;
        numberOfBoardings = 0;
        numberOfWalkingConnections = 0;
        hasBegun = false;
    }

    public static JourneyState fromPrevious(ImmutableJourneyState previousState) {
        return new JourneyState((JourneyState) previousState);
    }

    private JourneyState(JourneyState previousState) {
        this.journeyClock = previousState.journeyClock;
        this.transportMode = previousState.transportMode;
        this.journeyOffset = previousState.journeyOffset;
        this.traversalState = previousState.traversalState;
        if (onBoard()) {
            this.boardingTime = previousState.boardingTime;
        }
        this.numberOfBoardings = previousState.numberOfBoardings;
        this.numberOfWalkingConnections = previousState.numberOfWalkingConnections;
        this.hasBegun = previousState.hasBegun;
    }

    public static InitialBranchState<JourneyState> initialState(TramTime queryTime,
                                                                TraversalState traversalState) {
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
        return journeyClock;
    }


    public void updateTotalCost(int currentTotalCost) {
        int costForTrip = currentTotalCost - journeyOffset;

        if (onBoard()) {
            journeyClock = boardingTime.plusMinutes(costForTrip);
        } else {
            journeyClock = journeyClock.plusMinutes(costForTrip);
        }
    }

    public void recordTime(TramTime boardingTime, int currentCost) throws TramchesterException {
        if (! (onBoard()) ) {
            throw new TramchesterException("Not on a bus or tram");
        }
        this.journeyClock = boardingTime;
        this.boardingTime = boardingTime;
        this.journeyOffset = currentCost;
    }



    @Override
    public void beginWalk(Node beforeWalkNode, boolean atStart, int cost) {
        numberOfWalkingConnections = numberOfWalkingConnections + 1;
    }

    @Override
    public void beginTrip(IdFor<Trip> newTripId) {
        // noop
    }

    @Override
    public void endWalk(Node stationNode, boolean atDestination) {
        // noop
    }

    @Override
    public void toNeighbour(Node startNode, Node endNode, int cost) {
        numberOfWalkingConnections = numberOfWalkingConnections + 1;
    }

    private boolean onBoard() {
        return !transportMode.equals(TransportMode.NotSet);
    }

    @Override
    public void leave(TransportMode mode, int totalCost, Node node) throws TramchesterException {
        if (!transportMode.equals(mode)) {
            throw new TramchesterException("Not currently on " +mode+ " was " + transportMode);
        }
        leave(totalCost);
        transportMode = TransportMode.NotSet;
    }

    private void leave(int currentTotalCost) {
        if (currentTotalCost<journeyOffset) {
            throw new RuntimeException("Invalid total cost "+currentTotalCost+" less that current total offset " +journeyOffset);
        }

        int tripCost = currentTotalCost - journeyOffset;
        journeyClock = boardingTime.plusMinutes(tripCost);

        journeyOffset = currentTotalCost;
        boardingTime = null;
    }

    @Override
    public int getNumberChanges() {
        if (numberOfBoardings==0) {
            return 0;
        }
        return numberOfBoardings-1; // initial boarding
    }

    @Override
    public int getNumberWalkingConnections() {
        return numberOfWalkingConnections;
    }

    @Override
    public boolean hasBegunJourney() {
        return hasBegun;
    }

    @Override
    public void board(TransportMode mode, Node node, boolean hasPlatform) throws TramchesterException {
        guardAlreadyOnboard();
        numberOfBoardings = numberOfBoardings + 1;
        transportMode = mode;
        hasBegun = true;
    }

//    public void walkingConnection() {
//        numberOfWalkingConnections = numberOfWalkingConnections + 1;
//    }

    private void guardAlreadyOnboard() throws TramchesterException {
        if (!transportMode.equals(TransportMode.NotSet)) {
            throw new TramchesterException("Already on a " + transportMode);
        }
    }

    public TraversalState getTraversalState() {
        return traversalState;
    }

    public void updateTraversalState(TraversalState traversalState) {
        this.traversalState = traversalState;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JourneyState that = (JourneyState) o;
        return transportMode == that.transportMode &&
                Objects.equals(journeyClock, that.journeyClock);
    }

    @Override
    public int hashCode() {
        return Objects.hash(journeyClock, transportMode);
    }

    @Override
    public String toString() {
        return "JourneyState{" +
                "journeyClock=" + journeyClock +
                ", transportMode=" + transportMode +
                ", journeyOffset=" + journeyOffset +
                ", boardingTime=" + boardingTime +
                ", traversalState=" + traversalState +
                '}';
    }

    @Override
    public TransportMode getTransportMode() {
        return transportMode;
    }

}
