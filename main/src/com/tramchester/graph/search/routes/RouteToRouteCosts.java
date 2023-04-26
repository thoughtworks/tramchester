package com.tramchester.graph.search.routes;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.NumberOfChanges;
import com.tramchester.domain.Route;
import com.tramchester.domain.RoutePair;
import com.tramchester.domain.collections.IndexedBitSet;
import com.tramchester.domain.collections.RouteIndexPair;
import com.tramchester.domain.collections.RouteIndexPairFactory;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.places.InterchangeStation;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.graph.search.BetweenRoutesCostRepository;
import com.tramchester.graph.search.LowestCostsForDestRoutes;
import com.tramchester.repository.ClosedStationsRepository;
import com.tramchester.repository.NeighboursRepository;
import com.tramchester.repository.StationAvailabilityRepository;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

@LazySingleton
public class RouteToRouteCosts implements BetweenRoutesCostRepository {
    private static final Logger logger = LoggerFactory.getLogger(RouteToRouteCosts.class);

    public final static String INDEX_FILE = "route_index.json";

    private final NeighboursRepository neighboursRepository;
    private final StationAvailabilityRepository availabilityRepository;
    private final ClosedStationsRepository closedStationsRepository;
    private final RouteIndex index;
    private final RouteCostCombinations costs;
    private final RouteIndexPairFactory pairFactory;

    @Inject
    public RouteToRouteCosts(NeighboursRepository neighboursRepository,
                             StationAvailabilityRepository availabilityRepository,
                             ClosedStationsRepository closedStationsRepository,
                             RouteIndex index, RouteCostMatrix costs, RouteIndexPairFactory pairFactory) {
        this.neighboursRepository = neighboursRepository;
        this.availabilityRepository = availabilityRepository;
        this.closedStationsRepository = closedStationsRepository;
        this.index = index;
        this.costs = costs;

        this.pairFactory = pairFactory;
    }

    @PostConstruct
    public void start() {
        logger.info("starting");
        logger.info("started");
    }

    @PreDestroy
    public void stop() {
        logger.info("stopping");
        logger.info("stopped");
    }

    private int getNumberChangesFor(RoutePair routePair, TramDate date, StationAvailabilityFacade changeStationOperating,
                                    IndexedBitSet dateAndModeOverlaps) {
        if (routePair.areSame()) {
            return 0;
        }
        if (!routePair.bothAvailableOn(date)) {
            logger.debug(format("Routes %s not available on date %s", date, routePair));
            return Integer.MAX_VALUE;
        }

        RouteIndexPair routeIndexPair = index.getPairFor(routePair);
        final int result = getDepth(routeIndexPair, changeStationOperating, dateAndModeOverlaps);

        if (result == RouteCostMatrix.MAX_VALUE) {
            if (routePair.sameMode()) {
                // TODO Why so many hits here?
                // for mixed transport mode having no value is quite normal
                logger.debug("Missing " + routePair);
            }
            return Integer.MAX_VALUE;
        }
        return result;
    }

    private int getDepth(final RouteIndexPair routePair, final StationAvailabilityFacade changeStationOperating, final IndexedBitSet dateAndModeOverlaps) {

        // need to account for route availability and modes when getting the depth
        // the simple answer, or the lowest limit, comes from the matrix depth for the routePair
        // int min = costs.getDegree(routePair);
        // but that might not be available at the given date and mode

        // try from initialDepth to max depth to find any interchange that is operating at the given date/time/modes
        final int maxDepth = costs.getMaxDepth();
        final int initialDepth = costs.getDepth(routePair);

        if (initialDepth > maxDepth) {
            logger.warn("Initial depth " + initialDepth + " is greater than " + maxDepth);
            return Integer.MAX_VALUE;
        }

        final RouteCostMatrix.AnyOfPaths interchanges = costs.getInterchangesFor(routePair, dateAndModeOverlaps);

        final RouteCostMatrix.InterchangePath results = interchanges.filter(changeStationOperating::isOperating);

        if (results.hasAny()) {
            return results.getDepth();
        }

        logger.info("Found no operating station for " + HasId.asIds(index.getPairFor(routePair)));
        return Integer.MAX_VALUE;

    }

    public long size() {
        return costs.size();
    }

    @Override
    public NumberOfChanges getNumberOfChanges(StationGroup start, StationGroup end, TramDate date, TimeRange time, EnumSet<TransportMode> modes) {
        return getNumberOfChanges(LocationSet.of(start.getContained()), LocationSet.of(end.getContained()), date, time, modes);
    }

    @Override
    public NumberOfChanges getNumberOfChanges(LocationSet starts, LocationSet destinations, TramDate date, TimeRange timeRange,
                                              EnumSet<TransportMode> requestedModes) {

        Set<Route> startRoutes = pickupRoutesFor(starts, date, timeRange, requestedModes);
        Set<Route> endRoutes = dropoffRoutesFor(destinations, date, timeRange, requestedModes);

        if (startRoutes.isEmpty()) {
            logger.warn(format("start stations %s not available at %s and %s ", HasId.asIds(starts), date, timeRange));
            return NumberOfChanges.None();
        }
        if (endRoutes.isEmpty()) {
            logger.warn(format("destination stations %s not available at %s and %s ", HasId.asIds(starts), date, timeRange));
            return NumberOfChanges.None();
        }

        StationAvailabilityFacade interchangesOperating = new StationAvailabilityFacade(availabilityRepository, date, timeRange, requestedModes);

        if (neighboursRepository.areNeighbours(starts, destinations)) {
            return new NumberOfChanges(1, 1);
        }
        // todo account for closures, or covered by fact a set of locations is available here?
        return getNumberOfHops(startRoutes, endRoutes, date, interchangesOperating, 0, requestedModes);
    }

    @Override
    public NumberOfChanges getNumberOfChanges(Location<?> startStation, Location<?> destination,
                                              EnumSet<TransportMode> preferredModes, TramDate date, TimeRange timeRange) {

        if (preferredModes.isEmpty()) {
            throw new RuntimeException("Must provide preferredModes");
        }

        // should be captured correctly in the route matrix, but if filtering routes by transport mode/date/time-range
        // might miss a direct walk incorrectly at the start
        if (neighboursRepository.areNeighbours(startStation, destination)) {
            logger.info(format("Number of changes set to 1 since %s and %s are neighbours", startStation.getId(), destination.getId()));
            return new NumberOfChanges(1, 1);
        }

        boolean startClosed = closedStationsRepository.isClosed(startStation, date);
        boolean destClosed = closedStationsRepository.isClosed(destination, date);

        int closureOffset = (startClosed?1:0) + (destClosed?1:0);

        // Need to respect timing here, otherwise can find a route that is valid at an interchange but isn't
        // actually running from the start or destination
        final Set<Route> pickupRoutes = availabilityRepository.getPickupRoutesFor(startStation, date, timeRange, preferredModes);
        final Set<Route> dropoffRoutes = availabilityRepository.getDropoffRoutesFor(destination, date, timeRange, preferredModes);

        // TODO If the station is a partial closure or full closure AND walking diversions exist, then need
        // to calculate routes from those neighbours?
        // OR create fake routes?

        logger.info(format("Compute number of changes between %s (%s) and %s (%s) using modes '%s' on %s within %s",
                startStation.getId(), HasId.asIds(pickupRoutes), destination.getId(), HasId.asIds(dropoffRoutes),
                preferredModes, date, timeRange));

        StationAvailabilityFacade changeStationOperating = new StationAvailabilityFacade(availabilityRepository, date, timeRange, preferredModes);

        if (pickupRoutes.isEmpty()) {
            logger.warn(format("start station %s has no matching pick-up routes for %s %s %s",
                    startStation.getId(), date, timeRange, preferredModes));
            return NumberOfChanges.None();
        }
        if (dropoffRoutes.isEmpty()) {
            logger.warn(format("destination station %s has no matching drop-off routes for %s %s %s",
                    destination.getId(), date, timeRange, preferredModes));
            return NumberOfChanges.None();
        }

        return getNumberOfHops(pickupRoutes, dropoffRoutes, date, changeStationOperating, closureOffset, preferredModes);

    }

    @Override
    public NumberOfChanges getNumberOfChanges(Route routeA, Route routeB, TramDate date, TimeRange timeRange, EnumSet<TransportMode> requestedModes) {
        StationAvailabilityFacade interchangesOperating = new StationAvailabilityFacade(availabilityRepository, date, timeRange, requestedModes);
        return getNumberOfHops(Collections.singleton(routeA), Collections.singleton(routeB), date,
                interchangesOperating, 0, requestedModes);
    }

    @Override
    public LowestCostsForDestRoutes getLowestCostCalcutatorFor(LocationSet destinations, TramDate date, TimeRange timeRange,
                                                               EnumSet<TransportMode> requestedModes) {
        Set<Route> destinationRoutes = destinations.stream().
                map(dest -> availabilityRepository.getDropoffRoutesFor(dest, date, timeRange, requestedModes)).
                flatMap(Collection::stream).
                collect(Collectors.toUnmodifiableSet());
        return new LowestCostForDestinations(this, pairFactory, destinationRoutes, date, timeRange, requestedModes, availabilityRepository);
    }

    @NotNull
    private NumberOfChanges getNumberOfHops(Set<Route> startRoutes, Set<Route> destinationRoutes, TramDate date,
                                            StationAvailabilityFacade interchangesOperating,
                                            int closureOffset, EnumSet<TransportMode> requestedModes) {
        if (logger.isDebugEnabled()) {
            logger.debug(format("Compute number of changes between %s and %s on %s",
                    HasId.asIds(startRoutes), HasId.asIds(destinationRoutes), date));
        }

        IndexedBitSet dateAndModeOverlaps = costs.createOverlapMatrixFor(date, requestedModes);

        Set<RoutePair> routePairs = getRoutePairs(startRoutes, destinationRoutes);

        Set<Integer> numberOfChangesForRoutes = routePairs.stream().
                map(pair -> getNumberChangesFor(pair, date, interchangesOperating, dateAndModeOverlaps)).
                collect(Collectors.toSet());

        int maxDepth = RouteCostMatrix.MAX_DEPTH;

        int minHops = minHops(numberOfChangesForRoutes);
        if (minHops > maxDepth) {
            logger.error(format("Unexpected result for min hops %s greater than max depth %s, for %s to %s, change cache %s",
                    minHops, maxDepth, HasId.asIds(startRoutes), HasId.asIds(destinationRoutes), interchangesOperating));
        } else {
            minHops = minHops + closureOffset;
        }

        int maxHops = maxHops(numberOfChangesForRoutes);
        if (maxHops > maxDepth) {
            logger.error(format("Unexpected result for max hops %s greater than max depth %s, for %s to %s, change cache %s",
                    maxHops, maxDepth, HasId.asIds(startRoutes), HasId.asIds(destinationRoutes), interchangesOperating));
        } else {
            maxHops = maxHops + closureOffset;
        }

        NumberOfChanges numberOfChanges = new NumberOfChanges(minHops, maxHops);
        if (logger.isDebugEnabled()) {
            logger.debug(format("Computed number of changes from %s to %s on %s as %s",
                    HasId.asIds(startRoutes), HasId.asIds(destinationRoutes), date, numberOfChanges));
        }
        return numberOfChanges;
    }

    private int maxHops(Set<Integer> numberOfChangesForRoutes) {
        final Optional<Integer> query = numberOfChangesForRoutes.stream().
                filter(result -> result != Integer.MAX_VALUE).
                max(Integer::compare);

        if (query.isEmpty()) {
            logger.warn("No maxHops found from " + numberOfChangesForRoutes);
        }
        return query.orElse(Integer.MAX_VALUE);
    }

    private int minHops(Set<Integer> numberOfChangesForRoutes) {
        final Optional<Integer> query = numberOfChangesForRoutes.stream().
                min(Integer::compare);

        if (query.isEmpty()) {
            logger.warn("No minHops found for " + numberOfChangesForRoutes);
        }
        return query.orElse(Integer.MAX_VALUE);
    }

    @NotNull
    private Set<RoutePair> getRoutePairs(Set<Route> startRoutes, Set<Route> endRoutes) {
        // note: allow routeA -> routeA here, needed to correctly select minimum later on
        return startRoutes.stream().
                flatMap(startRoute -> endRoutes.stream().map(endRoute -> RoutePair.of(startRoute, endRoute))).
                collect(Collectors.toSet());
    }

    private Set<Route> dropoffRoutesFor(LocationSet locations, TramDate date, TimeRange timeRange, Set<TransportMode> modes) {
        return availabilityRepository.getDropoffRoutesFor(locations, date, timeRange, modes);
    }

    private Set<Route> pickupRoutesFor(LocationSet locations, TramDate date, TimeRange timeRange, Set<TransportMode> modes) {
        return availabilityRepository.getPickupRoutesFor(locations, date, timeRange, modes);
    }


    /***
     * Encapsulates lowest cost and hops for one specific set of destinations, required for performance reasons
     * as looking up destinations during the graph traversal was too costly
     */
    private static class LowestCostForDestinations implements LowestCostsForDestRoutes {
        private final RouteToRouteCosts routeToRouteCosts;
        private final RouteIndexPairFactory pairFactory;
        private final Set<Integer> destinationIndexs;
        private final StationAvailabilityFacade changeStationOperating;
        private final IndexedBitSet dateOverlaps;

        public LowestCostForDestinations(BetweenRoutesCostRepository routeToRouteCosts, RouteIndexPairFactory pairFactory, Set<Route> destinations,
                                         TramDate date, TimeRange time, EnumSet<TransportMode> requestedModes, StationAvailabilityRepository availabilityRepository) {
            this.routeToRouteCosts = (RouteToRouteCosts) routeToRouteCosts;
            this.pairFactory = pairFactory;
            destinationIndexs = destinations.stream().
                    map(destination -> this.routeToRouteCosts.index.indexFor(destination.getId())).
                    collect(Collectors.toUnmodifiableSet());

            changeStationOperating = new StationAvailabilityFacade(availabilityRepository, date, time, requestedModes);
            dateOverlaps = ((RouteToRouteCosts) routeToRouteCosts).costs.createOverlapMatrixFor(date, requestedModes);

        }

        /***
         * least number of "hops" between routes to reach a destination route
         * @param startingRoute current position
         * @return min number of hops needed to reach one of the destination routes
         */
        @Override
        public int getFewestChanges(Route startingRoute) {
            int indexOfStart = routeToRouteCosts.index.indexFor(startingRoute.getId());
            if (destinationIndexs.contains(indexOfStart)) {
                return 0;
            }


            // note: IntStream uses int in implementation so avoids any boxing overhead
            return destinationIndexs.stream().mapToInt(item -> item).
                    map(indexOfDest -> routeToRouteCosts.getDepth(pairFactory.get(indexOfStart, indexOfDest),
                            changeStationOperating, dateOverlaps)).
                    filter(result -> result != RouteCostMatrix.MAX_VALUE).
                    min().
                    orElse(Integer.MAX_VALUE);
        }

        @Override
        public <T extends HasId<Route>> Stream<T> sortByDestinations(Stream<T> startingRoutes) {
            return startingRoutes.
                    map(this::getLowestCost).
                    sorted(Comparator.comparingInt(Pair::getLeft)).
                    map(Pair::getRight);
        }

        @NotNull
        private <T extends HasId<Route>> Pair<Integer, T> getLowestCost(T start) {
            int indexOfStart = routeToRouteCosts.index.indexFor(start.getId());
            if (destinationIndexs.contains(indexOfStart)) {
                return Pair.of(0, start); // start on route that is present at destination
            }

            // note: IntStream uses int in implementation so avoids any boxing overhead
            int result = destinationIndexs.stream().mapToInt(item -> item).
                    map(dest -> routeToRouteCosts.getDepth(pairFactory.get(indexOfStart, dest), changeStationOperating, dateOverlaps)).
                    min().
                    orElse(Integer.MAX_VALUE);
            return Pair.of(result, start);
        }

    }

    /***
     * Needed for rail performance, significant
     */
    static class StationAvailabilityFacade {
        private final TramDate date;
        private final TimeRange time;
        private final EnumSet<TransportMode> modes;
        private final StationAvailabilityRepository availabilityRepository;

        private final Cache<Station, Boolean> cache;

        public StationAvailabilityFacade(StationAvailabilityRepository availabilityRepository, TramDate date, TimeRange time, EnumSet<TransportMode> modes) {
            this.availabilityRepository = availabilityRepository;
            this.date = date;
            this.time = time;
            this.modes = modes;

            cache = Caffeine.newBuilder().maximumSize(availabilityRepository.size()).expireAfterAccess(1, TimeUnit.MINUTES).
                    recordStats().build();
        }

        @Override
        public String toString() {
            return "StationAvailabilityFacade{" +
                    "date=" + date +
                    ", time=" + time +
                    ", modes=" + modes +
                    '}';
        }

        public Boolean isOperating(InterchangeStation interchangeStation) {
            Station station = interchangeStation.getStation();
            return cache.get(station, unused -> uncached(station));
        }

        private boolean uncached(Station station) {
            return availabilityRepository.isAvailable(station, date, time, modes);
        }
    }

}
