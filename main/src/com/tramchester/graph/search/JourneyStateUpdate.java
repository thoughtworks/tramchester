package com.tramchester.graph.search;

import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import org.neo4j.graphdb.Node;

public interface JourneyStateUpdate {
    void board(TransportMode transportMode, Node node) throws TramchesterException;
    void leave(TransportMode mode, int totalCost) throws TramchesterException;
    void walkingConnection();
    void updateJourneyClock(int total);
    void recordVehicleDetails(TramTime time, int totalCost) throws TramchesterException;
}
