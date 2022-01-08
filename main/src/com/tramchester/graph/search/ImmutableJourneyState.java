package com.tramchester.graph.search;

import com.tramchester.domain.HasTransportMode;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.search.stateMachine.states.TraversalState;

public interface ImmutableJourneyState extends HasTransportMode {
    TraversalState getTraversalState();
    String getTraversalStateName();
    TramTime getJourneyClock();
    int getNumberChanges();
    int getNumberWalkingConnections();
    boolean hasBegunJourney();
    int getNumberNeighbourConnections();
    int getTotalCostSoFar();
    boolean hasVisited(IdFor<Station> stationId);
}
