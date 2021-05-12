package com.tramchester.graph.search;

import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import org.neo4j.graphdb.Node;

public interface JourneyStateUpdate {
    void board(TransportMode transportMode, Node node, boolean hasPlatform) throws TramchesterException;
    void leave(TransportMode mode, int totalCost, Node node) throws TramchesterException;
    void walkingConnection();
    void updateJourneyClock(int total);
    void recordTime(TramTime time, int totalCost) throws TramchesterException;
    void beginTrip(IdFor<Trip> newTripId);
}
