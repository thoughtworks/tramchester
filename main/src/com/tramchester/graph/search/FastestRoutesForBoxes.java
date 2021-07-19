package com.tramchester.graph.search;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.BoundingBoxWithCost;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.JourneysForBox;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.BoundingBoxWithStations;
import com.tramchester.geo.FindNear;
import com.tramchester.geo.GridPosition;
import com.tramchester.geo.StationLocations;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
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
    public Stream<BoundingBoxWithCost> findForGrid(GridPosition destinationGrid, long gridSize, JourneyRequest journeyRequest) {
        logger.info("Creating station groups for gridsize " + gridSize + " and destination " + destinationGrid);

        Set<BoundingBoxWithStations> searchGrid = stationLocations.getGroupedStations(gridSize).collect(Collectors.toSet());

        BoundingBoxWithStations searchBoxWithDest = searchGrid.stream().
                filter(box -> box.contained(destinationGrid)).findFirst().
                orElseThrow(() -> new RuntimeException("Unable to find destination in any boxes " + destinationGrid));

        Set<Station> destinations = searchBoxWithDest.getStaions();

        logger.info(format("Using %s groups and %s destinations", searchGrid.size(), destinations.size()));

        List<BoundingBoxWithStations> sortedSearchGrid = sortGridNearestFirst(searchGrid, destinationGrid);
        return calculator.calculateRoutes(destinations, journeyRequest, sortedSearchGrid).
                map(box -> cheapest(box, destinationGrid));
    }

    private List<BoundingBoxWithStations> sortGridNearestFirst(Set<BoundingBoxWithStations> searchGrid, GridPosition destinationGrid) {
        return searchGrid.stream().
                sorted((a,b) -> FindNear.compareDistancesNearestFirst(destinationGrid, a.getMidPoint(), b.getMidPoint())).
                collect(Collectors.toList());
    }

    private BoundingBoxWithCost cheapest(JourneysForBox results, GridPosition destination) {

        if (results.contains(destination)) {
            return new BoundingBoxWithCost(results.getBox(), 0, null);
        }

        if (results.isEmpty()) {
            return new BoundingBoxWithCost(results.getBox(), -1, null);
        }

        Journey result = results.getLowestCost();

        int cost = TramTime.diffenceAsMinutes(result.getDepartTime(), result.getArrivalTime());
        return new BoundingBoxWithCost(results.getBox(), cost, result);
    }
}
