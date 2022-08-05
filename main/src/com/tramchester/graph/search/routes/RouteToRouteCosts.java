package com.tramchester.graph.search.routes;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.*;
import com.tramchester.domain.collections.SimpleList;
import com.tramchester.domain.collections.SimpleListSingleton;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.places.InterchangeStation;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.graph.search.BetweenRoutesCostRepository;
import com.tramchester.graph.search.LowestCostsForDestRoutes;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.NeighboursRepository;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.StationAvailabilityRepository;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.String.format;

@LazySingleton
public class RouteToRouteCosts implements BetweenRoutesCostRepository {
    private static final Logger logger = LoggerFactory.getLogger(RouteToRouteCosts.class);

    public final static String INDEX_FILE = "route_index.csv";

    public static final int MAX_DEPTH = 4;

    private final InterchangeRepository interchangeRepository;
    private final NeighboursRepository neighboursRepository;
    private final StationAvailabilityRepository availabilityRepository;
    private final RouteIndex index;

    private final RouteCostMatrix costs;
    private final int numberOfRoutes;

    @Inject
    public RouteToRouteCosts(RouteRepository routeRepository, InterchangeRepository interchangeRepository,
                             NeighboursRepository neighboursRepository, StationAvailabilityRepository availabilityRepository,
                             RouteIndex index) {
        this.interchangeRepository = interchangeRepository;
        this.neighboursRepository = neighboursRepository;

        numberOfRoutes = routeRepository.numberOfRoutes();
        this.availabilityRepository = availabilityRepository;
        this.index = index;
        costs = new RouteCostMatrix(availabilityRepository, routeRepository, this.index, numberOfRoutes, MAX_DEPTH + 1);
    }

    @PostConstruct
    public void start() {
        logger.info("starting");
        buildRouteConnectionMatrix();
        logger.info("started");
    }

    @PreDestroy
    public void stop() {
        logger.info("stopping");
        index.clear();
        costs.clear();
        logger.info("stopped");
    }

    private void buildRouteConnectionMatrix() {
        RouteDateAndDayOverlap routeDateAndDayOverlap = new RouteDateAndDayOverlap(index, numberOfRoutes);
        routeDateAndDayOverlap.populateFor();

        addInitialConnectionsFromInterchanges(routeDateAndDayOverlap);

        costs.start(routeDateAndDayOverlap);
    }

    private void addInitialConnectionsFromInterchanges(RouteDateAndDayOverlap routeDateAndDayOverlap) {
        final Set<InterchangeStation> interchanges = interchangeRepository.getAllInterchanges();
        logger.info("Pre-populate route to route costs from " + interchanges.size() + " interchanges");
        final IndexedBitSet forDegreeOne = costs.costsForDegree[1];
        interchanges.forEach(interchange -> addOverlapsForInterchange(forDegreeOne, interchange, routeDateAndDayOverlap));
        logger.info("Add " + costs.size() + " connections for interchanges");
    }

    private void addOverlapsForInterchange(IndexedBitSet forDegreeOne, InterchangeStation interchange,
                                           RouteDateAndDayOverlap routeDateAndDayOverlap) {

        // record interchanges, where we can go from being dropped off (routes) to being picked up (routes)
        final Set<Route> dropOffAtInterchange = interchange.getDropoffRoutes();
        final Set<Route> pickupAtInterchange = interchange.getPickupRoutes();

        for (final Route dropOff : dropOffAtInterchange) {
            final int dropOffIndex = index.indexFor(dropOff.getId());
            // todo, could use bitset Or and And with DateOverlapMask here
            for (final Route pickup : pickupAtInterchange) {
                if ((!dropOff.equals(pickup)) && pickup.isDateOverlap(dropOff)) {
                    final int pickupIndex = index.indexFor(pickup.getId());
                    forDegreeOne.set(dropOffIndex, pickupIndex);
                    costs.addInterchangeBetween(dropOffIndex, pickupIndex, interchange);
                }
            }
            // apply dates and days
            forDegreeOne.applyAndTo(dropOffIndex, routeDateAndDayOverlap.overlapsFor(dropOffIndex));
        }
    }

    /***
     * Use methods from BetweenRoutesCostRepository instead of this
     * @param routeA first route
     * @param routeB second route
     * @param timeRange the range within with the route needs ot be available
     * @return number of changes
     */
    public int getNumberChangesFor(Route routeA, Route routeB, LocalDate date, TimeRange timeRange) {
        IndexedBitSet dateOverlaps = costs.createOverlapMatrixFor(date);

        InterchangeOperating interchangeOperating = new InterchangeOperating(date, timeRange);

        RoutePair pair = RoutePair.of(routeA, routeB);
        return getNumberChangesFor(pair, date, interchangeOperating, dateOverlaps);
    }

    public int getNumberChangesFor(RoutePair routePair, LocalDate date, InterchangeOperating interchangeOperating,
                                   IndexedBitSet overlapsForDate) {
        if (routePair.areSame()) {
            return 0;
        }
        if (!routePair.isAvailableOn(date)) {
            logger.debug(format("Routes %s not available on date %s", date, routePair));
            return Integer.MAX_VALUE;
        }

        RouteIndexPair routeIndexPair = RouteIndexPair.getIndexPairFor(index, routePair);
        final int result = costs.getDepth(routeIndexPair, interchangeOperating, overlapsForDate);

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

    public List<List<RouteAndInterchanges>> getChangesFor(Route routeA, Route routeB) {
        RoutePair routePair = new RoutePair(routeA, routeB);

        logger.info("Get change stations betweem " + routePair);

        RouteIndexPair indexPair = RouteIndexPair.getIndexPairFor(index, routeA, routeB);

        IndexedBitSet dateOverlaps = IndexedBitSet.getIdentity(numberOfRoutes); // no specific date or time

        Collection<SimpleList<RouteIndexPair>> routeChanges = costs.getChangesFor(indexPair, dateOverlaps);

        List<List<RouteAndInterchanges>> interchanges = routeChanges.stream().
                map(list -> list.stream().map(costs::getInterchangeFor).filter(Objects::nonNull)).
                map(onePossibleSetOfChange -> onePossibleSetOfChange.collect(Collectors.toList()))
                .collect(Collectors.toList());

        if (routeChanges.isEmpty()) {
            logger.warn(format("Unable to find changes between %s", routePair));
        }
        return interchanges;

    }

    public int size() {
        return costs.size();
    }

    @Override
    public NumberOfChanges getNumberOfChanges(StationGroup start, StationGroup end, LocalDate date, TimeRange time) {
        return getNumberOfChanges(LocationSet.of(start.getContained()), LocationSet.of(end.getContained()), date, time);
    }

    @Override
    public NumberOfChanges getNumberOfChanges(LocationSet starts, LocationSet destinations, LocalDate date, TimeRange timeRange) {

        Set<Route> startRoutes = pickupRoutesFor(starts, date, timeRange);
        Set<Route> endRoutes = dropoffRoutesFor(destinations, date, timeRange);

        if (startRoutes.isEmpty()) {
            logger.warn(format("start stations %s not available at %s and %s ", HasId.asIds(starts), date, timeRange));
            return NumberOfChanges.None();
        }
        if (endRoutes.isEmpty()) {
            logger.warn(format("destination stations %s not available at %s and %s ", HasId.asIds(starts), date, timeRange));
            return NumberOfChanges.None();
        }

        InterchangeOperating interchangesOperating = new InterchangeOperating(date, timeRange);

        if (neighboursRepository.areNeighbours(starts, destinations)) {
            return new NumberOfChanges(1, 1);
        }
        return getNumberOfHops(startRoutes, endRoutes, date, interchangesOperating);
    }

    @Override
    public NumberOfChanges getNumberOfChanges(Location<?> startStation, Location<?> destination,
                                              Set<TransportMode> preferredModes, LocalDate date, TimeRange timeRange) {
        logger.info(format("Compute number of changes between %s and %s using modes '%s' on %s within %s",
                startStation.getId(), destination.getId(), preferredModes, date, timeRange));

        if (neighboursRepository.areNeighbours(startStation, destination)) {
            return new NumberOfChanges(1, 1);
        }

        // Need to respect timing here, otherwise can find a route that is valid at an interchange but isn't
        // actually running from the start or destination
        final Set<Route> pickupRoutes = availabilityRepository.getPickupRoutesFor(startStation,date, timeRange);
        final Set<Route> dropoffRoutes = availabilityRepository.getDropoffRoutesFor(destination, date, timeRange);

        InterchangeOperating interchangesOperating = new InterchangeOperating(date, timeRange);

        if (pickupRoutes.isEmpty()) {
            logger.warn(format("start station %s has no matching pick-up routes", startStation.getId()));
            return NumberOfChanges.None();
        }
        if (dropoffRoutes.isEmpty()) {
            logger.warn(format("destination station %s has no matching drop-off routes", destination.getId()));
            return NumberOfChanges.None();
        }

        if (preferredModes.isEmpty()) {
            return getNumberOfHops(pickupRoutes, dropoffRoutes, date, interchangesOperating);
        } else {
            final Set<Route> filteredPickupRoutes = filterForModes(preferredModes, pickupRoutes);
            final Set<Route> filteredDropoffRoutes = filterForModes(preferredModes, dropoffRoutes);

            if (filteredPickupRoutes.isEmpty() || filteredDropoffRoutes.isEmpty()) {
                logger.warn(format("No paths between routes %s and %s due to preferredModes modes %s, filtering gave %s and %s",
                        HasId.asIds(pickupRoutes), HasId.asIds(dropoffRoutes), preferredModes, HasId.asIds(filteredPickupRoutes),
                        HasId.asIds(filteredDropoffRoutes)));
                return NumberOfChanges.None();
            }

            return getNumberOfHops(filteredPickupRoutes, filteredDropoffRoutes, date, interchangesOperating);
        }

    }

    @NotNull
    private Set<Route> filterForModes(Set<TransportMode> modes, Set<Route> routes) {
        return routes.stream().filter(route -> modes.contains(route.getTransportMode())).collect(Collectors.toSet());
    }

    @Override
    public LowestCostsForDestRoutes getLowestCostCalcutatorFor(LocationSet destinations, LocalDate date, TimeRange timeRange) {
        Set<Route> destinationRoutes = destinations.stream().
                map(dest -> availabilityRepository.getDropoffRoutesFor(dest, date, timeRange)).
                flatMap(Collection::stream).
                collect(Collectors.toUnmodifiableSet());
        return new LowestCostForDestinations(this, destinationRoutes, date, timeRange);
    }

    @NotNull
    private NumberOfChanges getNumberOfHops(Set<Route> startRoutes, Set<Route> destinationRoutes, LocalDate date,
                                            InterchangeOperating interchangesOperating) {

        IndexedBitSet dateOverlaps = costs.createOverlapMatrixFor(date);

        Set<RoutePair> routePairs = getRoutePairs(startRoutes, destinationRoutes);

        Set<Integer> numberOfChangesForRoutes = routePairs.stream().
                map(pair -> getNumberChangesFor(pair, date, interchangesOperating, dateOverlaps)).
                collect(Collectors.toSet());

        int minHops = minHops(numberOfChangesForRoutes);
        if (minHops > MAX_DEPTH) {
            logger.error(format("Unexpected result for min hops %s greater than max depth %s, for %s to %s",
                    minHops, MAX_DEPTH, HasId.asIds(startRoutes), HasId.asIds(destinationRoutes)));
        }

        int maxHops = maxHops(numberOfChangesForRoutes);
        if (maxHops > MAX_DEPTH) {
            logger.error(format("Unexpected result for max hops %s greater than max depth %s, for %s to %s",
                    maxHops, MAX_DEPTH, HasId.asIds(startRoutes), HasId.asIds(destinationRoutes)));
        }

        NumberOfChanges numberOfChanges = new NumberOfChanges(minHops, maxHops);
        logger.info(format("Computed number of changes from %s to %s on %s as %s",
                HasId.asIds(startRoutes), HasId.asIds(destinationRoutes), date, numberOfChanges));
        return numberOfChanges;
    }

    private int maxHops(Set<Integer> numberOfChangesForRoutes) {
        final Optional<Integer> query = numberOfChangesForRoutes.stream().
                filter(result -> result != Integer.MAX_VALUE).
                max(Integer::compare);

        if (query.isEmpty()) {
            logger.warn("No maxHops found for " + numberOfChangesForRoutes);
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

    private Set<Route> dropoffRoutesFor(LocationSet locations, LocalDate date, TimeRange timeRange) {
        return availabilityRepository.getDropoffRoutesFor(locations, date, timeRange);
    }

    private Set<Route> pickupRoutesFor(LocationSet locations, LocalDate date, TimeRange timeRange) {
        return availabilityRepository.getPickupRoutesFor(locations, date, timeRange);
    }


    /***
     * Encapsulates lowest cost and hops for one specific set of destinations, required for performance reasons
     * as looking up destinations during the graph traversal was too costly
     */
    private static class LowestCostForDestinations implements LowestCostsForDestRoutes {
        private final RouteToRouteCosts routeToRouteCosts;
        private final Set<Integer> destinationIndexs;
        private final LocalDate date;
        private final TimeRange time;
        private final InterchangeOperating interchangeOperating;
        private final IndexedBitSet dateOverlaps;

        public LowestCostForDestinations(BetweenRoutesCostRepository routeToRouteCosts, Set<Route> destinations, LocalDate date, TimeRange time) {
            this.routeToRouteCosts = (RouteToRouteCosts) routeToRouteCosts;
            destinationIndexs = destinations.stream().
                    map(destination -> this.routeToRouteCosts.index.indexFor(destination.getId())).
                    collect(Collectors.toUnmodifiableSet());
            this.date = date;
            this.time = time;
            interchangeOperating = new InterchangeOperating(date, time);
            dateOverlaps = ((RouteToRouteCosts) routeToRouteCosts).costs.createOverlapMatrixFor(date);

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

            InterchangeOperating interchangeOperating = new InterchangeOperating(date, time);

            // note: IntStream uses int in implementation so avoids any boxing overhead
            return destinationIndexs.stream().mapToInt(item -> item).
                    map(indexOfDest -> routeToRouteCosts.costs.getDepth(RouteIndexPair.of(indexOfStart, indexOfDest),
                            interchangeOperating, dateOverlaps)).
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
                    map(dest -> routeToRouteCosts.costs.getDepth(RouteIndexPair.of(indexOfStart, dest), interchangeOperating,
                            dateOverlaps)).
                    min().
                    orElse(Integer.MAX_VALUE);
            return Pair.of(result, start);
        }

    }

    public static class RouteIndexPair {
        private final int first;
        private final int second;
        private final int hashCode;

        private RouteIndexPair(int first, int second) {
            this.first = first;
            this.second = second;
            hashCode = Objects.hash(first, second);
        }

        public static RouteIndexPair getIndexPairFor(RouteIndex index, RoutePair pair) {
            int a = index.indexFor(pair.getFirst().getId());
            int b = index.indexFor(pair.getSecond().getId());
            return of(a, b);
        }

        public static RouteIndexPair getIndexPairFor(RouteIndex index, Route routeA, Route routeB) {
            int a = index.indexFor(routeA.getId());
            int b = index.indexFor(routeB.getId());
            return of(a, b);
        }


        @Override
        public String toString() {
            return "RouteIndexPair{" +
                    "first=" + first +
                    ", second=" + second +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RouteIndexPair routePair = (RouteIndexPair) o;
            return first == routePair.first && second == routePair.second;
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        public static RouteIndexPair of(int first, int second) {
            return new RouteIndexPair(first, second);
        }

        public int first() {
            return first;
        }

        public int second() {
            return second;
        }

        public boolean isSame() {
            return first == second;
        }
    }

    /***
     * Caches whether an interchange is available at a specific date and time range
     */
    private static class InterchangeOperating {
        private final LocalDate date;
        private final TimeRange time;

        final private Set<RouteAndInterchanges> active;

        public InterchangeOperating(LocalDate date, TimeRange time) {

            this.date = date;
            this.time = time;
            active = new HashSet<>();
        }

        public boolean isOperating(StationAvailabilityRepository availabilityRepository, List<RouteAndInterchanges> changeSet) {
            return changeSet.stream().anyMatch(item -> isOperating(availabilityRepository, item));
        }

        private boolean isOperating(StationAvailabilityRepository availabilityRepository, RouteAndInterchanges routeAndInterchanges) {
            if (active.contains(routeAndInterchanges)) {
                return true;
            }
            boolean available = availabilityRepository.isAvailable(routeAndInterchanges, date, time);
            if (available) {
                active.add(routeAndInterchanges);
            }
            return available;
        }
    }

    private static class RouteCostMatrix {
        public static final byte MAX_VALUE = Byte.MAX_VALUE;

        // map from route->route pair to interchanges that could make that change
        private final Map<RouteIndexPair, Set<Station>> routePairToInterchange;
        private final StationAvailabilityRepository availabilityRepository;
        private final RouteRepository routeRepository;

        private final IndexedBitSet[] costsForDegree;
        private final RouteIndex index;
        private final int maxDepth;
        private final int numRoutes;

        private RouteCostMatrix(StationAvailabilityRepository availabilityRepository, RouteRepository routeRepository,
                                RouteIndex index, int numRoutes, int maxDepth) {
            this.availabilityRepository = availabilityRepository;
            this.routeRepository = routeRepository;
            this.index = index;
            this.maxDepth = maxDepth;
            costsForDegree = new IndexedBitSet[maxDepth];
            routePairToInterchange = new HashMap<>();
            for (int degree = 1; degree < maxDepth; degree++) {
                costsForDegree[degree] = new IndexedBitSet(numRoutes);
            }
            this.numRoutes = numRoutes;
        }

        // TODO pass in the map? Or construct in this class
        public void addInterchangeBetween(int dropOffIndex, int pickupIndex, InterchangeStation interchange) {
            RouteIndexPair key = RouteIndexPair.of(dropOffIndex, pickupIndex);
            if (!routePairToInterchange.containsKey(key)) {
                routePairToInterchange.put(key, new HashSet<>());
            }
            routePairToInterchange.get(key).add(interchange.getStation());
        }

        // create a bitmask for route->route changes that are possible on a given date
        private IndexedBitSet createOverlapMatrixFor(LocalDate date) {
            final Set<Integer> availableOnDate = new HashSet<>();
            for (int routeIndex = 0; routeIndex < numRoutes; routeIndex++) {
                final Route route = index.getRouteFor(routeIndex);
                if (route.isAvailableOn(date)) {
                    availableOnDate.add(routeIndex);
                }
            }
            IndexedBitSet matrix = new IndexedBitSet(numRoutes);
            for (int firstRouteIndex = 0; firstRouteIndex < numRoutes; firstRouteIndex++) {
                BitSet result = new BitSet(numRoutes);
                if (availableOnDate.contains(firstRouteIndex)) {
                    for(int secondRouteIndex = 0; secondRouteIndex < numRoutes; secondRouteIndex++) {
                        if (availableOnDate.contains(secondRouteIndex)) {
                            result.set(secondRouteIndex);
                        }
                    }
                }
                matrix.insert(firstRouteIndex, result);
            }
            availableOnDate.clear();
            return matrix;
        }

        public int size() {
            int result = 0;
            for (int i = 1; i < maxDepth; i++) {
                result = result + costsForDegree[i].numberOfConnections();
            }
            return result;
        }

        private int getDepth(RouteIndexPair routePair, InterchangeOperating interchangeOperating, IndexedBitSet dateOverlaps) {

            Collection<SimpleList<RouteIndexPair>> possibleChanges = getChangesFor(routePair, dateOverlaps);

            List<List<RouteAndInterchanges>> filteredByAvailability = new ArrayList<>();

            int smallestSeen = Integer.MAX_VALUE;
            for (SimpleList<RouteIndexPair> listOfChanges : possibleChanges) {
                final int numberOfChanges = listOfChanges.size();
                if (numberOfChanges < smallestSeen) {
                    List<RouteAndInterchanges> listOfInterchanges = getRouteAndInterchange(listOfChanges);
                    final boolean available = interchangeOperating.isOperating(availabilityRepository, listOfInterchanges);
                    if (available) {
                        filteredByAvailability.add(listOfInterchanges);
                        smallestSeen = numberOfChanges;
                    }
                }
            }

            if (possibleChanges.size()!=filteredByAvailability.size()) {
                logger.debug(format("Filtered from %s to %s", possibleChanges.size(), filteredByAvailability.size()));
            } else {
                logger.debug("Retained " + filteredByAvailability.size());
            }

            Optional<Integer> result = filteredByAvailability.stream().
                    map(List::size).
                    min(Integer::compare);

            return result.orElse(Integer.MAX_VALUE);

        }

        private List<RouteAndInterchanges> getRouteAndInterchange(SimpleList<RouteIndexPair> listOfChanges) {
            return listOfChanges.stream().
                    map(this::getInterchangeFor).
                    filter(Objects::nonNull).
                    collect(Collectors.toList());
        }

        private byte getDegree(RouteIndexPair routePair) {
            if (routePair.isSame()) {
                return 0;
            }
            for (int depth = 1; depth < maxDepth; depth++) {
                if (costsForDegree[depth].isSet(routePair.first, routePair.second)) {
                    return (byte) depth;
                }
            }
            return MAX_VALUE;
        }

        private IndexedBitSet costsFor(int currentDegree) {
            return costsForDegree[currentDegree];
        }

        public void clear() {
            for (int degree = 1; degree < maxDepth; degree++) {
                costsForDegree[degree].clear();
            }
        }

        /***
         * @param routePair indexes for first and second routes
         * @param dateOverlaps bit mask indicating that routeA->routeB is available at date
         * @return each set returned contains specific interchanges between 2 specific routes
         */
        private Collection<SimpleList<RouteIndexPair>> getChangesFor(final RouteIndexPair routePair, IndexedBitSet dateOverlaps) {

            final byte initialDepth = getDegree(routePair);

            if (initialDepth == 0) {
                //logger.warn(format("getChangesFor: No changes needed indexes %s", routePair));
                return Collections.emptySet();
            }
            if (initialDepth == Byte.MAX_VALUE) {
                logger.debug(format("getChangesFor: no changes possible indexes %s", routePair));
                return Collections.emptySet();
            }

            if (logger.isDebugEnabled()) {
                logger.debug(format("Expand for %s initial depth %s", routePair, initialDepth));
            }

            Collection<SimpleList<RouteIndexPair>> possibleInterchangePairs = expandOnePair(routePair, initialDepth, dateOverlaps);

            if (logger.isDebugEnabled()) {
                logger.debug(format("Got %s set of changes for %s: %s", possibleInterchangePairs.size(), routePair, possibleInterchangePairs));
            }

            return possibleInterchangePairs;
        }

        private Collection<SimpleList<RouteIndexPair>> expandOnePair(final RouteIndexPair original, final int degree, final IndexedBitSet dateOverlaps) {
            if (degree == 1) {
                // at degree one we are at direct connections between routes via an interchange so the result is those pairs
                //logger.debug("degree 1, expand pair to: " + original);
                return Collections.singleton(new SimpleListSingleton<>(original));
            }

            final int nextDegree = degree - 1;
            final IndexedBitSet overlapsAtDegree = this.costsForDegree[nextDegree]; //.and(dateOverlaps);

            final List<SimpleList<RouteIndexPair>> resultsForPair = new ArrayList<>();

            // for >1 result is the set of paris where each of the supplied pairs overlaps
            final List<Integer> overlappingIndexes = getIndexOverlapsFor(overlapsAtDegree, original);
            overlappingIndexes.forEach(overlapForPair -> {

                Pair<RouteIndexPair, RouteIndexPair> toExpand = formNewRoutePairs(original, overlapForPair);

                if (dateOverlaps.isSet(toExpand.getLeft()) && dateOverlaps.isSet(toExpand.getRight())) {
                    final Collection<SimpleList<RouteIndexPair>> leftExpansions = expandOnePair(toExpand.getLeft(), nextDegree, dateOverlaps);
                    final Collection<SimpleList<RouteIndexPair>> rightExpansions = expandOnePair(toExpand.getRight(), nextDegree, dateOverlaps);

                    final Collection<SimpleList<RouteIndexPair>> resultsForOneOverlap = new ArrayList<>(leftExpansions.size()*rightExpansions.size());

                    leftExpansions.forEach(leftExpansion -> rightExpansions.forEach(rightExpansion -> {
                        final SimpleList<RouteIndexPair> combined = SimpleList.concat(RouteIndexPair.class, leftExpansion, rightExpansion);
                        resultsForOneOverlap.add(combined);
                    }));
                    resultsForPair.addAll(resultsForOneOverlap);
                }

            });

            if (logger.isDebugEnabled()) {
                logger.debug(format("Result of expanding %s => %s", original, resultsForPair));
            }

            return resultsForPair;

        }

        @NotNull
        private Pair<RouteIndexPair, RouteIndexPair> formNewRoutePairs(RouteIndexPair pair, int overlapForPair) {
            RouteIndexPair newFirstPair = RouteIndexPair.of(pair.first, overlapForPair);
            RouteIndexPair newSecondPair = RouteIndexPair.of(overlapForPair, pair.second);
            return Pair.of(newFirstPair, newSecondPair);
        }

        private RouteAndInterchanges getInterchangeFor(RouteIndexPair indexPair) {

            if (routePairToInterchange.containsKey(indexPair)) {
                final RoutePair routePair = index.getPairFor(indexPair);

                final Set<Station> changes = routePairToInterchange.get(indexPair);
                final RouteAndInterchanges routeAndInterchanges = new RouteAndInterchanges(routePair, changes);
                if (logger.isDebugEnabled()) {
                    logger.debug(format("Found changes %s for %s", HasId.asIds(changes), indexPair));
                }
                return routeAndInterchanges;
            }
            logger.debug("Did not find any interchanges for " + indexPair);
            return null;
        }

        /***
         * computes index (bits set) for the overlap between 2 routes and at given degree
         * @param costsForDegree bitmap for current degree/cost
         * @param pair the 2 routes to compute the overlap for
         * @return list of places in the bitmap where the routes overlap aka the indexes of the routes in that row
         */
        private List<Integer> getIndexOverlapsFor(IndexedBitSet costsForDegree, RouteIndexPair pair) {
            ImmutableBitSet linksForA = costsForDegree.getBitSetForRow(pair.first);
            ImmutableBitSet linksForB = costsForDegree.getBitSetForRow(pair.second);
            ImmutableBitSet overlap = linksForA.and(linksForB);
            return overlap.stream().boxed().collect(Collectors.toList());
        }


        public void start(RouteDateAndDayOverlap routeDateAndDayOverlap) {
            populateCosts(routeDateAndDayOverlap);
        }

        private void populateCosts(RouteDateAndDayOverlap routeDateAndDayOverlap) {
            final int size = routeRepository.numberOfRoutes();
            logger.info("Find costs between " + size + " routes");
            final int fullyConnected = size * size;


            for (byte currentDegree = 1; currentDegree < MAX_DEPTH; currentDegree++) {
                addConnectionsFor(routeDateAndDayOverlap, currentDegree);
                final int currentTotal = size();
                logger.info("Total number of connections " + currentTotal);
                if (currentTotal >= fullyConnected) {
                    break;
                }
            }

            final int finalSize = size();
            logger.info("Added cost for " + finalSize + " route combinations");
            if (finalSize < fullyConnected) {
                double percentage = ((double) finalSize / (double) fullyConnected);
                logger.warn(format("Not fully connected, only %s (%s) of %s ", finalSize, percentage, fullyConnected));
            }
        }


        private void addConnectionsFor(RouteDateAndDayOverlap routeDateAndDayOverlap, byte currentDegree) {
            final Instant startTime = Instant.now();
            final int nextDegree = currentDegree + 1;

            final IndexedBitSet currentMatrix = costsFor(currentDegree);
            final IndexedBitSet newMatrix = costsFor(nextDegree);

            for (int routeIndex = 0; routeIndex < numRoutes; routeIndex++) {

                final ImmutableBitSet currentConnectionsForRoute = currentMatrix.getBitSetForRow(routeIndex);
                final BitSet resultForForRoute = new BitSet(numRoutes);
                for (int connectionIndex = 0; connectionIndex < numRoutes; connectionIndex++) {
                    if (currentConnectionsForRoute.get(connectionIndex)) {
                        // if current routeIndex is connected to a route, then for next degree include that other routes connections
                        final ImmutableBitSet otherRoutesConnections = currentMatrix.getBitSetForRow(connectionIndex);
                        otherRoutesConnections.applyOr(resultForForRoute);
                    }
                }
                final BitSet dateOverlapMask = routeDateAndDayOverlap.overlapsFor(routeIndex);  // only those routes whose dates overlap
                resultForForRoute.and(dateOverlapMask);
                currentConnectionsForRoute.applyAndNot(resultForForRoute);  // don't include any current connections for this route
                newMatrix.insert(routeIndex, resultForForRoute);
            }

            final long took = Duration.between(startTime, Instant.now()).toMillis();
            logger.info("Added connections " + newMatrix.numberOfConnections() + "  Degree " + nextDegree + " in " + took + " ms");
        }
    }

    private static class RouteDateAndDayOverlap {
        // Create a bitmask corresponding to the dates and days routes overlap
        // NOTE: this is for route overlaps only, it does cover whether specific stations
        // are served by the routes on a specific date

        private final BitSet[] overlapMasks;
        private final int numberOfRoutes;
        private final RouteIndex index;

        private RouteDateAndDayOverlap(RouteIndex index, int numberOfRoutes) {
            this.index = index;
            overlapMasks = new BitSet[numberOfRoutes];
            this.numberOfRoutes = numberOfRoutes;
        }

        public void populateFor() {
            logger.info("Creating matrix for route date/day overlap");

            for (int i = 0; i < numberOfRoutes; i++) {
                final Route from = index.getRouteFor(i);
                BitSet resultsForRoute = new BitSet(numberOfRoutes);
                final int fromIndex = i;
                // thread safety: split into list and then application of list to bitset
                List<Integer> toSet = IntStream.range(0, numberOfRoutes).
                        parallel().
                        filter(toIndex -> (fromIndex == toIndex) || from.isDateOverlap(index.getRouteFor(toIndex))).
                        boxed().collect(Collectors.toList());
                toSet.forEach(resultsForRoute::set);
                overlapMasks[i] = resultsForRoute;
            }
            logger.info("Finished matrix for route date/day overlap");
        }

        public BitSet overlapsFor(int routeIndex) {
            return overlapMasks[routeIndex];
        }
    }

}
