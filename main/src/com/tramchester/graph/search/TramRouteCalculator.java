package com.tramchester.graph.search;

import com.tramchester.domain.Journey;
import com.tramchester.domain.Station;
import com.tramchester.domain.StationWalk;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import org.neo4j.graphdb.Node;

import java.util.List;
import java.util.stream.Stream;

public interface TramRouteCalculator {
    Stream<Journey> calculateRoute(String startStationId, Station destination, JourneyRequest journeyRequest);

    Stream<Journey> calculateRouteWalkAtEnd(String startId, Node destination, List<Station> destStations, JourneyRequest journeyRequest);

    Stream<Journey> calculateRouteWalkAtStart(Node startOfWalkNode, Station destination, JourneyRequest journeyRequest);
}
