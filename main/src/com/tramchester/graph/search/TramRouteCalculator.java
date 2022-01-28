package com.tramchester.graph.search;

import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.NumberOfChanges;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.StationWalk;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import java.util.Set;
import java.util.stream.Stream;

public interface TramRouteCalculator {
    Stream<Journey> calculateRoute(Transaction txn, Location<?> startStation, Location<?> destination, JourneyRequest journeyRequest);

    Stream<Journey> calculateRouteWalkAtEnd(Transaction txn, Location<?> start, Node destination, LocationSet destStations,
                                            JourneyRequest journeyRequest, NumberOfChanges numberOfChanges);

    Stream<Journey> calculateRouteWalkAtStart(Transaction txn, Set<StationWalk> stationWalks, Node startOfWalkNode, Location<?> destination,
                                              JourneyRequest journeyRequest, NumberOfChanges numberOfChanges);

    Stream<Journey> calculateRouteWalkAtStartAndEnd(Transaction txn, Set<StationWalk> stationWalks, Node startNode, Node endNode,
                                                    LocationSet destinationStations,
                                                    JourneyRequest journeyRequest, NumberOfChanges numberOfChanges);
}
