package com.tramchester.testSupport;

import com.tramchester.domain.Journey;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.repository.StationRepository;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.Transaction;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RouteCalculatorTestFacade {
    private final RouteCalculator theCalulcator;
    private final StationRepository repository;
    private final Transaction txn;

    public RouteCalculatorTestFacade(RouteCalculator theCalulcator, StationRepository repository, Transaction txn) {
       this.theCalulcator = theCalulcator;
        this.repository = repository;
        this.txn = txn;
    }

    public Set<Journey> validateAtLeastNJourney(int maxToReturn, JourneyRequest journeyRequest, Station start, Station destination) {
        Set<Journey> journeys = calculateRouteAsSet(maxToReturn, journeyRequest, start, destination);

        String message = "from " + start.getName() + " to " + destination.getName() + " at " + journeyRequest.getTime() + " on " + journeyRequest.getDate();
        assertTrue(journeys.size() > 0, "Unable to find journey " + message);
        journeys.forEach(journey -> assertFalse(journey.getStages().isEmpty(), message + " missing stages for journey" + journey));
        journeys.forEach(RouteCalculatorTestFacade::checkStages);
        return journeys;
    }

    private static void checkStages(Journey journey) {
        List<TransportStage> stages = journey.getStages();
        TramTime earliestAtNextStage = null;
        for (TransportStage stage : stages) {
            if (earliestAtNextStage!=null) {
                assertFalse(
                        stage.getFirstDepartureTime().isBefore(earliestAtNextStage), stage.toString() + " arrived before " + earliestAtNextStage);
            }
            earliestAtNextStage = stage.getFirstDepartureTime().plusMinutes(stage.getDuration());
        }
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

    @NotNull
    public Set<Journey> calculateRouteAsSet(int maxToReturn, JourneyRequest journeyRequest,
                                            Station start, Station destination) {
        Stream<Journey> journeyStream = theCalulcator.calculateRoute(txn,
                repository.getStationById(start.getId()),
                repository.getStationById(destination.getId()), journeyRequest);
        Set<Journey> journeys = journeyStream.limit(maxToReturn).collect(Collectors.toSet());
        journeyStream.close();
        return journeys;
    }

    private Station real(TestStations start) {
        return TestStation.real(repository, start);
    }


}
