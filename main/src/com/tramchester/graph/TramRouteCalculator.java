package com.tramchester.graph;

import com.tramchester.domain.Journey;
import com.tramchester.domain.StationWalk;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import org.neo4j.graphdb.Node;

import java.util.List;
import java.util.stream.Stream;

public interface TramRouteCalculator {
    Stream<Journey> calculateRoute(String startStationId, String destinationId, TramTime queryTime,
                                   TramServiceDate queryDate);

    Stream<Journey> calculateRouteWalkAtEnd(String startId, Node destination, List<String> destStations, TramTime queryTime,
                                            TramServiceDate queryDate);

    Stream<Journey> calculateRouteWalkAtStart(Node startOfWalkNode, String destinationId, TramTime queryTime,
                                              TramServiceDate queryDate);
}
