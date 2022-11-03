package com.tramchester.graph.search;

import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import org.neo4j.graphdb.Node;

import java.time.Duration;

public interface JourneyStateUpdate {
    void board(TransportMode transportMode, Node node, boolean hasPlatform) throws TramchesterException;
    void leave(IdFor<Trip> trip, TransportMode mode, Duration totalCost, Node node) throws TramchesterException;

    void beginTrip(IdFor<Trip> newTripId);

    void beginWalk(Node beforeWalkNode, boolean atStart, Duration cost);
    void endWalk(Node stationNode);

    void toNeighbour(Node startNode, Node endNode, Duration cost);
    void seenStation(IdFor<Station> stationId);

    void updateTotalCost(Duration total);
    void recordTime(TramTime time, Duration totalCost) throws TramchesterException;
}
