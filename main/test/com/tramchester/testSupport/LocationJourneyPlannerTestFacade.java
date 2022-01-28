package com.tramchester.testSupport;

import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.repository.StationRepository;
import com.tramchester.resources.LocationJourneyPlanner;
import com.tramchester.testSupport.reference.FakeStation;
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

    public Set<Journey> quickestRouteForLocation(LatLong start, Location<?> dest, JourneyRequest request, int maxStages) {
        return asSetClosed(planner.quickestRouteForLocation(txn, start, dest, request), maxStages);
    }

    public Set<Journey> quickestRouteForLocation(Location<?> start, LatLong dest, JourneyRequest request, int maxStages) {
        return asSetClosed(planner.quickestRouteForLocation(txn, start, dest, request), maxStages);
    }

    public Set<Journey> quickestRouteForLocation(FakeStation start, Location<?> dest, JourneyRequest request, int maxStages) {
        return quickestRouteForLocation(start.from(stationRepository), dest.getLatLong(), request, maxStages);
    }

    public Set<Journey> quickestRouteForLocation(Location<?> start, FakeStation dest, JourneyRequest request, int maxStages) {
        return quickestRouteForLocation(start, dest.from(stationRepository), request, maxStages);
    }

    public Set<Journey> quickestRouteForLocation(LatLong latLong, FakeStation destination, JourneyRequest journeyRequest, int maxStages) {
        return quickestRouteForLocation(latLong, destination.from(stationRepository), journeyRequest, maxStages);
    }

    public Set<Journey> quickestRouteForLocation(FakeStation start, LatLong dest, JourneyRequest journeyRequest, int maxStages) {
        return quickestRouteForLocation(start.from(stationRepository), dest, journeyRequest, maxStages);
    }

    ////

    public Set<Journey> quickestRouteForLocation(LatLong start, LatLong dest, JourneyRequest request, int maxStages) {
        return asSetClosed(planner.quickestRouteForLocation(txn, start, dest, request), maxStages);
    }

    public Set<Journey> quickestRouteForLocation(Location<?> start, Location<?> dest, JourneyRequest request, int maxStages) {
        return asSetClosed(planner.quickestRouteForLocation(txn, start.getLatLong(), dest.getLatLong(), request), maxStages);
    }

    @NotNull
    private Set<Journey> asSetClosed(Stream<Journey> theStream, int maxStages) {
        Set<Journey> result = theStream.
                filter(journey -> journey.getStages().size()<=maxStages).
                collect(Collectors.toSet());
        theStream.close();
        return result;
    }


}
