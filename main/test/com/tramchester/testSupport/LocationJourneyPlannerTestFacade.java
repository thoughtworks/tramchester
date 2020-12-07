package com.tramchester.testSupport;

import com.tramchester.domain.Journey;
import com.tramchester.domain.places.PostcodeLocation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.repository.StationRepository;
import com.tramchester.resources.LocationJourneyPlanner;
import com.tramchester.testSupport.reference.BusStations;
import com.tramchester.testSupport.reference.TramStations;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.Transaction;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LocationJourneyPlannerTestFacade {
    private final LocationJourneyPlanner planner;
    private final StationRepository stationRepository;
    private final Transaction txn;

    public LocationJourneyPlannerTestFacade(LocationJourneyPlanner thePlanner, StationRepository stationRepository, Transaction txn) {
        this.planner = thePlanner;
        this.stationRepository = stationRepository;
        this.txn = txn;
    }

    public Set<Journey> quickestRouteForLocation(LatLong start, Station dest, JourneyRequest request, int maxStages) {
        return asSetClosed(planner.quickestRouteForLocation(txn, start, dest, request), maxStages);
    }

    public Set<Journey> quickestRouteForLocation(Station start, LatLong dest, JourneyRequest request, int maxStages) {
        return asSetClosed(planner.quickestRouteForLocation(txn, start, dest, request), maxStages);
    }

    public Set<Journey> quickestRouteForLocation(PostcodeLocation start, PostcodeLocation dest, JourneyRequest request, int maxStages) {
        return asSetClosed(planner.quickestRouteForLocation(txn, start.getLatLong(), dest.getLatLong(), request), maxStages);
    }

    public Set<Journey> quickestRouteForLocation(BusStations start, PostcodeLocation dest, JourneyRequest request, int maxStages) {
        return quickestRouteForLocation(getReal(start), dest.getLatLong(), request, maxStages);
    }

    public Set<Journey> quickestRouteForLocation(PostcodeLocation start, BusStations dest, JourneyRequest request, int maxStages) {
        return quickestRouteForLocation(start.getLatLong(), getReal(dest), request, maxStages);
    }

    public Set<Journey> quickestRouteForLocation(LatLong start, TramStations dest, JourneyRequest request, int maxStages) {
        return quickestRouteForLocation(start, getReal(dest), request, maxStages);
    }

    public Set<Journey> quickestRouteForLocation(TramStations start, LatLong dest, JourneyRequest request, int maxStages) {
        return quickestRouteForLocation(getReal(start), dest, request, maxStages);
    }

    public Set<Journey> quickestRouteForLocation(LatLong start, LatLong dest, JourneyRequest request, int maxStages) {
        return asSetClosed(planner.quickestRouteForLocation(txn, start, dest, request), maxStages);
    }

    private Station getReal(BusStations testStation) {
        return TestStation.real(stationRepository, testStation);
    }

    private Station getReal(TramStations testStation) {
        return TestStation.real(stationRepository, testStation);
    }

    @NotNull
    private Set<Journey> asSetClosed(Stream<Journey> theStream, int maxStages) {
        Set<Journey> result = theStream.filter(journey -> journey.getStages().size()<=maxStages).collect(Collectors.toSet());
        theStream.close();
        return result;
    }

}
