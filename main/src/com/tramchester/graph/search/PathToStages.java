package com.tramchester.graph.search;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.TransportStage;
import org.neo4j.graphdb.Transaction;

import java.util.List;
import java.util.Set;

@ImplementedBy(MapPathToStagesViaStates.class)
public interface PathToStages {
    List<TransportStage<?, ?>> mapDirect(Transaction txn, RouteCalculator.TimedPath timedPath, JourneyRequest journeyRequest,
                                         Set<Station> endStations);
}
