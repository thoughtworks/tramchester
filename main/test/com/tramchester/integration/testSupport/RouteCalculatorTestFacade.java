package com.tramchester.integration.testSupport;

import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.reference.FakeStation;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.Transaction;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RouteCalculatorTestFacade {
    private final RouteCalculator routeCalculator;
    private final StationRepository stationRepository;
    private final Transaction txn;

    public RouteCalculatorTestFacade(RouteCalculator routeCalculator, StationRepository stationRepository, Transaction txn) {
       this.routeCalculator = routeCalculator;
        this.stationRepository = stationRepository;
        this.txn = txn;
    }

    public Set<Journey> calculateRouteAsSet(FakeStation start, FakeStation end, JourneyRequest journeyRequest) {
        return calculateRouteAsSet(start.from(stationRepository), end.from(stationRepository), journeyRequest);
    }

    public Set<Journey> calculateRouteAsSet(IdFor<Station> startId, IdFor<Station> destId, JourneyRequest request) {
        return calculateRouteAsSet(getFor(startId), getFor(destId), request);
    }

    public Set<Journey> calculateRouteAsSet(FakeStation start, StationGroup end, JourneyRequest journeyRequest) {
        return calculateRouteAsSet(start.from(stationRepository), end, journeyRequest);
    }

    @NotNull
    public Set<Journey> calculateRouteAsSet(Location<?> start, Location<?> dest, JourneyRequest request) {
        Stream<Journey> stream = routeCalculator.calculateRoute(txn, start, dest, request);
        Set<Journey> result = stream.collect(Collectors.toSet());
        stream.close();
        return result;
    }

    private Station getFor(IdFor<Station> id) {
        return stationRepository.getStationById(id);
    }

}
