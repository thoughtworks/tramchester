package com.tramchester.testSupport;

import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.repository.StationRepository;
import com.tramchester.resources.LocationJourneyPlanner;
import com.tramchester.testSupport.reference.FakeStation;
import com.tramchester.testSupport.reference.KnownLocations;
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


    public Set<Journey> quickestRouteForLocation(Location<?> start, KnownLocations dest, JourneyRequest journeyRequest, int maxStages) {
        return quickestRouteForLocation(start, dest.location(), journeyRequest, maxStages);
    }

    public Set<Journey> quickestRouteForLocation(KnownLocations start, FakeStation dest, JourneyRequest journeyRequest, int maxStages) {
        return quickestRouteForLocation(start.location(), dest.from(stationRepository), journeyRequest, maxStages);
    }

    public Set<Journey> quickestRouteForLocation(FakeStation start, Location<?> dest, JourneyRequest request, int maxStages) {
        return quickestRouteForLocation(start.from(stationRepository), dest, request, maxStages);
    }

    public Set<Journey> quickestRouteForLocation(KnownLocations knownLocations, StationGroup end, JourneyRequest journeyRequest, int maxStages) {
        return quickestRouteForLocation(knownLocations.location(), end, journeyRequest, maxStages);
    }

    public Set<Journey> quickestRouteForLocation(Location<?> start, FakeStation dest, JourneyRequest request, int maxStages) {
        return quickestRouteForLocation(start, dest.from(stationRepository), request, maxStages);
    }


    public Set<Journey> quickestRouteForLocation(FakeStation start, KnownLocations dest, JourneyRequest journeyRequest, int maxStages) {
        return quickestRouteForLocation(start.from(stationRepository), dest.location(), journeyRequest, maxStages);
    }

    public Set<Journey> quickestRouteForLocation(Location<?> start, Location<?> dest, JourneyRequest request, int maxStages) {
        return asSetClosed(planner.quickestRouteForLocation(txn, start, dest, request), maxStages);
    }

    @NotNull
    private Set<Journey> asSetClosed(Stream<Journey> theStream, int maxStages) {
        Set<Journey> result = theStream.
                filter(journey -> journey.getStages().size()<=maxStages).
                collect(Collectors.toSet());
        theStream.close();
        return result;
    }

    public Set<Journey> quickestRouteForLocation(KnownLocations knownLocation, Station dest, JourneyRequest journeyRequest, int maxStages) {
        return quickestRouteForLocation(knownLocation.location(), dest, journeyRequest, maxStages);
    }
}
