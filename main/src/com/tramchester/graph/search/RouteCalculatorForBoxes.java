package com.tramchester.graph.search;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.BoundingBoxWithStations;
import com.tramchester.geo.SortsPositions;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphQuery;
import com.tramchester.graph.RouteCostCalculator;
import com.tramchester.graph.caches.LowestCostSeen;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.search.diagnostics.ReasonsToGraphViz;
import com.tramchester.graph.search.stateMachine.states.TraversalStateFactory;
import com.tramchester.repository.ClosedStationsRepository;
import com.tramchester.repository.RouteInterchangeRepository;
import com.tramchester.repository.RunningRoutesAndServices;
import com.tramchester.repository.TransportData;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

@LazySingleton
public class RouteCalculatorForBoxes extends RouteCalculatorSupport {
    private static final Logger logger = LoggerFactory.getLogger(RouteCalculatorForBoxes.class);

    private final TramchesterConfig config;
    private final GraphDatabase graphDatabaseService;
    private final ClosedStationsRepository closedStationsRepository;
    private final RunningRoutesAndServices runningRoutesAndService;

    @Inject
    public RouteCalculatorForBoxes(TramchesterConfig config,
                                   TransportData transportData,
                                   GraphDatabase graphDatabaseService, GraphQuery graphQuery, TraversalStateFactory traversalStateFactory,
                                   PathToStages pathToStages,
                                   NodeContentsRepository nodeContentsRepository,
                                   ProvidesNow providesNow,
                                   SortsPositions sortsPosition, MapPathToLocations mapPathToLocations,
                                   BetweenRoutesCostRepository routeToRouteCosts, ReasonsToGraphViz reasonToGraphViz,
                                   ClosedStationsRepository closedStationsRepository, RunningRoutesAndServices runningRoutesAndService,
                                   RouteInterchangeRepository routeInterchanges, RouteCostCalculator routeCostCalculator) {
        super(graphQuery, pathToStages, nodeContentsRepository, graphDatabaseService,
                traversalStateFactory, providesNow, sortsPosition, mapPathToLocations,
                transportData, config, transportData, routeToRouteCosts, reasonToGraphViz, routeInterchanges, routeCostCalculator);
        this.config = config;
        this.graphDatabaseService = graphDatabaseService;
        this.closedStationsRepository = closedStationsRepository;
        this.runningRoutesAndService = runningRoutesAndService;
    }

    public Stream<JourneysForBox> calculateRoutes(LocationSet destinations, JourneyRequest journeyRequest,
                                                  List<BoundingBoxWithStations> grouped) {
        logger.info("Finding routes for bounding boxes");

        // TODO Compute over a range of times
        final TramTime originalTime = journeyRequest.getOriginalTime();

        final EnumSet<TransportMode> requestedModes = journeyRequest.getRequestedModes();

        TramDate date = journeyRequest.getDate();
        final LowestCostsForDestRoutes lowestCostForDestinations = routeToRouteCosts.getLowestCostCalcutatorFor(destinations, date,
                journeyRequest.getTimeRange(), requestedModes);
        final RunningRoutesAndServices.FilterForDate routeAndServicesFilter = runningRoutesAndService.getFor(date);

        final IdSet<Station> closedStations = closedStationsRepository.getFullyClosedStationsFor(date).stream().
                map(ClosedStation::getStationId).collect(IdSet.idCollector());

        final Duration maxJourneyDuration = journeyRequest.getMaxJourneyDuration();
        final JourneyConstraints journeyConstraints = new JourneyConstraints(config, routeAndServicesFilter, closedStations,
                destinations, lowestCostForDestinations, maxJourneyDuration);

        final Set<Long> destinationNodeIds = getDestinationNodeIds(destinations);

        return grouped.parallelStream().map(box -> {

            logger.info(format("Finding shortest path for %s --> %s for %s", box, destinations, journeyRequest));
            final LocationSet startingStations = LocationSet.of(box.getStations());
            final LowestCostSeen lowestCostSeenForBox = new LowestCostSeen();

            final NumberOfChanges numberOfChanges = computeNumberOfChanges(startingStations, destinations, date, journeyRequest.getTimeRange(), requestedModes);

            try(Transaction txn = graphDatabaseService.beginTx()) {

                Stream<Journey> journeys = startingStations.stream().
                        filter(start -> !destinations.contains(start)).
                        map(start -> new NodeAndStation(start, getLocationNodeSafe(txn, start))).
                        flatMap(nodeAndStation -> numChangesRange(journeyRequest, numberOfChanges).
                                map(numChanges -> createPathRequest(nodeAndStation.node, date, originalTime, requestedModes, numChanges,
                                        journeyConstraints, getMaxInitialWaitFor(nodeAndStation.location, config)))).
                        flatMap(pathRequest -> findShortestPath(txn, destinationNodeIds, destinations,
                                createServiceReasons(journeyRequest, originalTime), pathRequest, journeyConstraints.getFewestChangesCalculator(),
                                createPreviousVisits(), lowestCostSeenForBox)).
                        map(timedPath -> createJourney(journeyRequest, timedPath, destinations, lowestCostForDestinations));

                Set<Journey> collect = journeys.
                        filter(journey -> !journey.getStages().isEmpty()).
                        limit(journeyRequest.getMaxNumberOfJourneys()).
                        collect(Collectors.toSet());

                // yielding
                return new JourneysForBox(box, collect);
            }
        });

    }

    private static class NodeAndStation {

        private final Location<?> location;
        private final Node node;

        public NodeAndStation(Location<?> location, Node node) {

            this.location = location;
            this.node = node;
        }
    }

    private NumberOfChanges computeNumberOfChanges(LocationSet starts, LocationSet destinations, TramDate date, TimeRange timeRange, EnumSet<TransportMode> modes) {
        return routeToRouteCosts.getNumberOfChanges(starts, destinations, date, timeRange, modes);
    }
}
