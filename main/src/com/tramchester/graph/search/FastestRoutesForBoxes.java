package com.tramchester.graph.search;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.*;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.BoundingBoxWithStations;
import com.tramchester.geo.GridPosition;
import com.tramchester.geo.StationLocations;
import com.tramchester.mappers.Geography;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.Duration;
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
    private final Geography geography;

    @Inject
    public FastestRoutesForBoxes(StationLocations stationLocations, RouteCalculatorForBoxes calculator, Geography geography) {
        this.stationLocations = stationLocations;
        this.calculator = calculator;
        this.geography = geography;
    }

    public Stream<BoundingBoxWithCost> findForGrid(Station destination, long gridSize, JourneyRequest journeyRequest)  {

        logger.info("Creating station groups for gridsize " + gridSize + " and destination " + destination);
        GridPosition gridPosition = destination.getGridPosition();

        return findForGrid(gridPosition, gridSize, journeyRequest);
    }

    @NotNull
    public Stream<BoundingBoxWithCost> findForGrid(GridPosition destinationGrid, long gridSize, JourneyRequest journeyRequest) {
        logger.info("Creating station groups for gridsize " + gridSize + " and destination " + destinationGrid);

        Set<BoundingBoxWithStations> searchGrid = stationLocations.getStationsInGrids(gridSize).collect(Collectors.toSet());

        BoundingBoxWithStations searchBoxWithDest = searchGrid.stream().
                filter(box -> box.contained(destinationGrid)).findFirst().
                orElseThrow(() -> new RuntimeException("Unable to find destination in any boxes " + destinationGrid));

        LocationSet destinations = LocationSet.of(searchBoxWithDest.getStations());

        logger.info(format("Using %s groups and %s destinations", searchGrid.size(), destinations.size()));

        List<BoundingBoxWithStations> sortedSearchGrid = sortGridNearestFirst(searchGrid, destinationGrid);
        return calculator.calculateRoutes(destinations, journeyRequest, sortedSearchGrid).
                map(box -> cheapest(box, destinationGrid));
    }

    private List<BoundingBoxWithStations> sortGridNearestFirst(Set<BoundingBoxWithStations> searchGrid, GridPosition destinationGrid) {
        return searchGrid.stream().
                sorted((a,b) -> geography.chooseNearestToGrid(destinationGrid, a.getMidPoint(), b.getMidPoint())).
                collect(Collectors.toList());
    }

    private BoundingBoxWithCost cheapest(JourneysForBox results, GridPosition destination) {

        if (results.contains(destination)) {
            return new BoundingBoxWithCost(results.getBox(), Duration.ZERO, null);
        }

        if (results.isEmpty()) {
            return new BoundingBoxWithCost(results.getBox(), Duration.ofMinutes(-1), null);
        }

        Journey result = results.getLowestCost();

        Duration cost = TramTime.difference(result.getDepartTime(), result.getArrivalTime());
        return new BoundingBoxWithCost(results.getBox(), cost, result);
    }
}
