package com.tramchester.graph;

import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.states.TraversalState;

public interface ImmutableJourneyState {
    TraversalState getTraversalState();
    TramTime getJourneyClock();
}
