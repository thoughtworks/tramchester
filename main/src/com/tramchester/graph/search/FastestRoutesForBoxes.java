package com.tramchester.graph.search;

import com.tramchester.domain.BoundingBoxWithCost;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneysForBox;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.BoundingBoxWithStations;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.HasGridPosition;
import com.tramchester.geo.StationLocations;
import org.jetbrains.annotations.NotNull;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

public class FastestRoutesForBoxes {
    private static final Logger logger = LoggerFactory.getLogger(FastestRoutesForBoxes.class);

    private final StationLocations stationLocations;
    private final RouteCalculator calculator;

    public FastestRoutesForBoxes(StationLocations stationLocations, RouteCalculator calculator) {
        this.stationLocations = stationLocations;
        this.calculator = calculator;
    }

    public Stream<BoundingBoxWithCost> findForGridSizeAndDestination(Station destination, long gridSize,
                                                                     JourneyRequest journeyRequest)  {

        logger.info("Creating station groups for gridsize " + gridSize);
        List<BoundingBoxWithStations> grouped = stationLocations.getGroupedStations(gridSize).collect(Collectors.toList());

        Optional<BoundingBoxWithStations> searchBoxWithDest = grouped.stream().
                filter(box -> box.contained(destination.getGridPosition())).findFirst();

        if (searchBoxWithDest.isEmpty()) {
            throw new RuntimeException("Unable to find destination in any boxes " + destination);
        }
        Set<Station> destinations = searchBoxWithDest.get().getStaions();

        logger.info(format("Using %s groups and %s destinations", grouped.size(), destinations.size()));
        return calculator.calculateRoutes(destinations, journeyRequest, grouped).
                map(box->cheapest(box, destination.getGridPosition()));
    }

    private BoundingBoxWithCost cheapest(JourneysForBox journeysForBox, HasGridPosition destination) {

        if (journeysForBox.getBox().contained(destination)) {
            return new BoundingBoxWithCost(journeysForBox.getBox(), 0, null);
        }

        if (journeysForBox.getJourneys().isEmpty()) {
            logger.warn("No journeys for " + journeysForBox.getBox());
            return new BoundingBoxWithCost(journeysForBox.getBox(), -1, null);
        }

        int currentLowestCost = Integer.MAX_VALUE;
        Journey currentBest = null;

        for (Journey journey: journeysForBox.getJourneys()) {
            TramTime depart = getFirstDepartureTime(journey.getStages());
            TramTime arrive = getExpectedArrivalTime(journey.getStages());
            int duration = TramTime.diffenceAsMinutes(depart, arrive);
            if (duration < currentLowestCost) {
                currentBest = journey;
                currentLowestCost = duration;
            }
        }

        return new BoundingBoxWithCost(journeysForBox.getBox(), currentLowestCost, currentBest);
    }

    // TODO into journey???
    // See also JourneyDTOFactory
    private TramTime getFirstDepartureTime(List<TransportStage> allStages) {
        if (allStages.size() == 0) {
            return TramTime.midnight();
        }
        return getFirstStage(allStages).getFirstDepartureTime();
    }

    private TramTime getExpectedArrivalTime(List<TransportStage> allStages) {
        if (allStages.size() == 0) {
            return TramTime.of(0,0);
        }
        return getLastStage(allStages).getExpectedArrivalTime();
    }

    private TransportStage getLastStage(List<TransportStage> allStages) {
        int index = allStages.size()-1;
        return allStages.get(index);
    }

    private TransportStage getFirstStage(List<TransportStage> allStages) {
        return allStages.get(0);
    }

}
