package com.tramchester.testSupport;

import com.tramchester.domain.Journey;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.repository.StationRepository;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.Transaction;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RouteCalculatorTestFacade {
    private final RouteCalculator theCalulcator;
    private final StationRepository repository;
    private final Transaction txn;

    public RouteCalculatorTestFacade(RouteCalculator theCalulcator, StationRepository repository, Transaction txn) {
       this.theCalulcator = theCalulcator;
        this.repository = repository;
        this.txn = txn;
    }

    public Set<Journey> calculateRouteAsSet(TestStations start, TestStations dest, JourneyRequest request) {
        return calculateRouteAsSet(real(start), real(dest), request);
    }

    @NotNull
    public Set<Journey> calculateRouteAsSet(Station start, Station dest, JourneyRequest request) {
        Stream<Journey> stream = theCalulcator.calculateRoute(txn, start, dest, request);
        Set<Journey> result = stream.collect(Collectors.toSet());
        stream.close();
        return result;
    }

    public Set<Journey> calculateRouteAsSet(TestStations start, TestStations dest, JourneyRequest request, int maxToReturn) {
        Stream<Journey> stream = theCalulcator.calculateRoute(txn, real(start), real(dest), request);
        Set<Journey> result = stream.limit(maxToReturn).collect(Collectors.toSet());
        stream.close();
        return result;
    }

    public Set<Journey> calculateRouteAsSet(IdFor<Station> start, IdFor<Station> dest, JourneyRequest request, int maxToReturn) {
        Stream<Journey> stream = theCalulcator.calculateRoute(txn, get(start), get(dest), request);
        Set<Journey> result = stream.limit(maxToReturn).collect(Collectors.toSet());
        stream.close();
        return result;
    }

    private Station get(IdFor<Station> id) {
        return repository.getStationById(id);
    }

    private Station real(TestStations start) {
        return TestStation.real(repository, start);
    }


}
