package com.tramchester.graph.search;

import com.tramchester.domain.Journey;
import com.tramchester.domain.places.Station;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import java.util.List;
import java.util.stream.Stream;

public interface TramRouteCalculator {
    Stream<Journey> calculateRoute(Transaction txn, Station startStation, Station destination, JourneyRequest journeyRequest);

    Stream<Journey> calculateRouteWalkAtEnd(Transaction txn, Station start, Node destination, List<Station> destStations, JourneyRequest journeyRequest);

    Stream<Journey> calculateRouteWalkAtStart(Transaction txn, Node startOfWalkNode, Station destination, JourneyRequest journeyRequest);

    Stream<Journey> calculateRouteWalkAtStartAndEnd(Transaction txn, Node startNode, Node endNode, List<Station> destinationStations,
                                                    JourneyRequest journeyRequest);
}
