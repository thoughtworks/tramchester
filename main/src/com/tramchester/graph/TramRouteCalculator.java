package com.tramchester.graph;

import com.tramchester.domain.Journey;
import com.tramchester.domain.StationWalk;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;

import java.util.List;
import java.util.stream.Stream;

public interface TramRouteCalculator {
    Stream<Journey> calculateRoute(String startStationId, String destinationId, TramTime queryTime,
                                   TramServiceDate queryDate);

    Stream<Journey> calculateRouteWalkAtEnd(String startId, LatLong destination, List<StationWalk> walksToDest,
                                            TramTime queryTime, TramServiceDate queryDate);

    Stream<Journey> calculateRouteWalkAtStart(LatLong origin, List<StationWalk> walksToStartStations, String destinationId,
                                              TramTime queryTime, TramServiceDate queryDate);
}
