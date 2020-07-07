package com.tramchester.graph.search;

import com.tramchester.domain.BoundingBoxWithCost;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneysForBox;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.BoundingBoxWithStations;
import com.tramchester.geo.StationLocations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
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
                                                                     JourneyRequest journeyRequest) {

        Set<Station> destinations = stationLocations.getStationsRangeInMeters(destination, gridSize);

        logger.info("Creating station groups for gridsize " + gridSize);
        List<BoundingBoxWithStations> grouped = stationLocations.getGroupedStations(gridSize).collect(Collectors.toList());

        logger.info(format("Using %s groups and %s destinations", grouped.size(), destinations.size()));
        return calculator.calculateRoutes(destinations, journeyRequest, grouped).map(this::cheapest);
    }

    private BoundingBoxWithCost cheapest(JourneysForBox journeysForBox) {
        int currentQuickest = Integer.MAX_VALUE;

        for (Journey journey: journeysForBox.getJourneys()) {
            TramTime depart = getFirstDepartureTime(journey.getStages());
            TramTime arrive = getExpectedArrivalTime(journey.getStages());
            int duration = TramTime.diffenceAsMinutes(depart, arrive);
            if (duration < currentQuickest) {
                currentQuickest = duration;
            }
        }
        if (journeysForBox.getJourneys().isEmpty()) {
            currentQuickest = -1;
        }

        return new BoundingBoxWithCost(journeysForBox.getBox(), currentQuickest);
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
