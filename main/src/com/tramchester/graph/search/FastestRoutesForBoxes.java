package com.tramchester.graph.search;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.BoundingBoxWithCost;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneysForBox;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.BoundingBoxWithStations;
import com.tramchester.geo.GridPosition;
import com.tramchester.geo.StationLocations;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

@LazySingleton
public class FastestRoutesForBoxes {
    private static final Logger logger = LoggerFactory.getLogger(FastestRoutesForBoxes.class);

    private final StationLocations stationLocations;
    private final RouteCalculatorForBoxes calculator;

    @Inject
    public FastestRoutesForBoxes(StationLocations stationLocations, RouteCalculatorForBoxes calculator) {
        this.stationLocations = stationLocations;
        this.calculator = calculator;
    }

    public Stream<BoundingBoxWithCost> findForGrid(Station destination, long gridSize, JourneyRequest journeyRequest)  {

        logger.info("Creating station groups for gridsize " + gridSize + " and destination " + destination);
        GridPosition gridPosition = destination.getGridPosition();

        return findForGrid(gridPosition, gridSize, journeyRequest);
    }

    @NotNull
    public Stream<BoundingBoxWithCost> findForGrid(GridPosition destination, long gridSize, JourneyRequest journeyRequest) {
        logger.info("Creating station groups for gridsize " + gridSize + " and destination " + destination);
        List<BoundingBoxWithStations> grouped = stationLocations.getGroupedStations(gridSize).collect(Collectors.toList());

        Optional<BoundingBoxWithStations> searchBoxWithDest = grouped.stream().
                filter(box -> box.contained(destination)).findFirst();

        if (searchBoxWithDest.isEmpty()) {
            throw new RuntimeException("Unable to find destination in any boxes " + destination);
        }
        Set<Station> destinations = searchBoxWithDest.get().getStaions();

        logger.info(format("Using %s groups and %s destinations", grouped.size(), destinations.size()));
        return calculator.calculateRoutes(destinations, journeyRequest, grouped).
                map(box -> cheapest(box, destination));
    }

    private BoundingBoxWithCost cheapest(JourneysForBox journeysForBox, GridPosition destination) {

        if (journeysForBox.getBox().contained(destination)) {
            return new BoundingBoxWithCost(journeysForBox.getBox(), 0, null);
        }

        if (journeysForBox.getJourneys().isEmpty()) {
            logger.info("No journeys for " + journeysForBox.getBox());
            return new BoundingBoxWithCost(journeysForBox.getBox(), -1, null);
        }

        Optional<Journey> findEarliest = journeysForBox.getJourneys().stream().min(Comparator.comparing(Journey::getArrivalTime));
        if (findEarliest.isPresent()) {
            Journey journey = findEarliest.get();
            int cost = TramTime.diffenceAsMinutes(journey.getDepartTime(), journey.getArrivalTime());
            return new BoundingBoxWithCost(journeysForBox.getBox(), cost, journey);
        } else {
            logger.error("Could not find any cheapest journey for " + journeysForBox);
            return new BoundingBoxWithCost(journeysForBox.getBox(), -1, null);
        }

    }
}
