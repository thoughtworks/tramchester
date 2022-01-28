package com.tramchester.integration.testSupport;

import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestStations;
import com.tramchester.testSupport.reference.BusStations;
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

    public Set<Journey> calculateRouteAsSet(TestStations start, TestStations dest, JourneyRequest request) {
        return calculateRouteAsSet(get(start), get(dest), request);
    }

    public Set<Journey> calculateRouteAsSet(IdFor<Station> start, IdFor<Station> dest, JourneyRequest request) {
        return calculateRouteAsSet(get(start), get(dest), request);
    }

    public Set<Journey> calculateRouteAsSet(StationGroup start, TestStations end, JourneyRequest journeyRequest) {
        return calculateRouteAsSet(start, get(end), journeyRequest);
    }

    public Set<Journey> calculateRouteAsSet(BusStations start, StationGroup end, JourneyRequest journeyRequest) {
        return calculateRouteAsSet(get(start), end, journeyRequest);
    }

    @NotNull
    public Set<Journey> calculateRouteAsSet(Location<?> start, Location<?> dest, JourneyRequest request) {
        Stream<Journey> stream = routeCalculator.calculateRoute(txn, start, dest, request);
        Set<Journey> result = stream.collect(Collectors.toSet());
        stream.close();
        return result;
    }

    private Station get(IdFor<Station> id) {
        return stationRepository.getStationById(id);
    }

    private Station get(TestStations start) {
        return start.from(stationRepository);
    }
}
