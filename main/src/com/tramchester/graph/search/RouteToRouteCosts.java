package com.tramchester.graph.search;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.caching.DataCache;
import com.tramchester.dataexport.DataSaver;
import com.tramchester.dataimport.data.RouteIndexData;
import com.tramchester.domain.*;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.InterchangeStation;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.graph.filters.GraphFilterActive;
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

    private final RouteRepository routeRepository;
    private final InterchangeRepository interchangeRepository;
    private final NeighboursRepository neighboursRepository;
    private final StationAvailabilityRepository availabilityRepository;

    private final Costs costs;
    private final RouteIndex index;
    private final int numberOfRoutes;
    private final GraphFilterActive graphFilter;
    private final DataCache dataCache;

    @Inject
    public RouteToRouteCosts(RouteRepository routeRepository, InterchangeRepository interchangeRepository,
                             NeighboursRepository neighboursRepository, StationAvailabilityRepository availabilityRepository, DataCache dataCache, GraphFilterActive graphFilter) {
        this.routeRepository = routeRepository;
        this.interchangeRepository = interchangeRepository;
        this.neighboursRepository = neighboursRepository;

        numberOfRoutes = routeRepository.numberOfRoutes();
        this.availabilityRepository = availabilityRepository;
        this.graphFilter = graphFilter;
        this.dataCache = dataCache;
        index = new RouteIndex(routeRepository);
        costs = new Costs(availabilityRepository, index, numberOfRoutes, MAX_DEPTH + 1);
    }

    @PostConstruct
    public void start() {
        logger.info("starting");
        if (graphFilter.isActive()) {
            logger.warn("Filtering is enabled, skipping all caching");
            index.populateFrom(routeRepository);
        } else {
            if (dataCache.has(index)) {
                dataCache.loadInto(index, RouteIndexData.class);
            } else {
                index.populateFrom(routeRepository);
                dataCache.save(index, RouteIndexData.class);
            }
        }
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
        populateCosts(routeDateAndDayOverlap);
    }

    private void populateCosts(RouteDateAndDayOverlap routeDateAndDayOverlap) {
        final int size = routeRepository.numberOfRoutes();
        logger.info("Find costs between " + size + " routes");
        final int fullyConnected = size * size;

        addInitialConnectionsFromInterchanges(routeDateAndDayOverlap);

        for (byte currentDegree = 1; currentDegree < MAX_DEPTH; currentDegree++) {
            addConnectionsFor(routeDateAndDayOverlap, currentDegree);
            final int currentTotal = costs.size();
            logger.info("Total number of connections " + currentTotal);
            if (currentTotal >= fullyConnected) {
                break;
            }
        }

        final int finalSize = costs.size();
        logger.info("Added cost for " + finalSize + " route combinations");
        if (finalSize < fullyConnected) {
            double percentage = ((double) finalSize / (double) fullyConnected);
            logger.warn(format("Not fully connected, only %s (%s) of %s ", finalSize, percentage, fullyConnected));
        }
    }

    private void addConnectionsFor(RouteDateAndDayOverlap routeDateAndDayOverlap, byte currentDegree) {
        final Instant startTime = Instant.now();
        final int nextDegree = currentDegree + 1;

        final IndexedBitSet currentMatrix = costs.costsFor(currentDegree);
        final IndexedBitSet newMatrix = costs.costsFor(nextDegree);

        for (int routeIndex = 0; routeIndex < numberOfRoutes; routeIndex++) {

            ImmutableBitSet currentConnectionsForRoute = currentMatrix.getBitSetForRow(routeIndex);
            BitSet result = new BitSet(numberOfRoutes);
            for (int connectionIndex = 0; connectionIndex < numberOfRoutes; connectionIndex++) {
                if (currentConnectionsForRoute.get(connectionIndex)) {
                    // if current routeIndex is connected to a route, then for next degree include that other routes connections
                    ImmutableBitSet otherRoutesConnections = currentMatrix.getBitSetForRow(connectionIndex);
                    otherRoutesConnections.applyOr(result);
                }
            }
            final BitSet dateOverlapMask = routeDateAndDayOverlap.overlapsFor(routeIndex);  // only those routes whose dates overlap
            result.and(dateOverlapMask);
            currentConnectionsForRoute.applyAndNot(result);  // don't include any current connections for this route
            newMatrix.insert(routeIndex, result);
        }

        final long took = Duration.between(startTime, Instant.now()).toMillis();
        logger.info("Added connections " + newMatrix.numberOfConnections() + "  Degree " + nextDegree + " in " + took + " ms");
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

        return getNumberChangesFor(routeA, routeB, date, timeRange, interchangeOperating, dateOverlaps);
    }

    public int getNumberChangesFor(Route routeA, Route routeB, LocalDate date, TimeRange timeRange,
                                   InterchangeOperating interchangeOperating, IndexedBitSet overlapsForDate) {
        if (routeA.equals(routeB)) {
            return 0;
        }
        if (!routeA.isDateOverlap(routeB)) {
            logger.debug(format("No date overlap between %s and %s", routeA.getId(), routeB.getId()));
            return Integer.MAX_VALUE;
        }

        RouteIndexPair routePair = RouteIndexPair.getIndexPairFor(index, routeA, routeB);
        final int result = costs.getDepth(routePair, date, timeRange, interchangeOperating, overlapsForDate);

        if (result == Costs.MAX_VALUE) {
            if (routeA.getTransportMode() == routeB.getTransportMode()) {
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

        List<List<RouteAndInterchanges>> result = costs.getChangesFor(indexPair, dateOverlaps);

        if (result.isEmpty()) {
            logger.warn(format("Unable to find changes between %s", routePair));
        }
        return result;

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
        // TODO optimise this
        if (!availabilityRepository.isAvailable(starts, date, timeRange)) {
        //if (starts.stream().allMatch(station -> station.getPickupRoutes(date, timeRange).isEmpty())) {
            logger.warn(format("start stations %s not available at %s and %s ", HasId.asIds(starts), date, timeRange));
            return NumberOfChanges.None();
        }
        // TODO optimise this
        if (!availabilityRepository.isAvailable(destinations, date, timeRange)) {
        //if (destinations.stream().allMatch(station -> station.getDropoffRoutes(date, timeRange).isEmpty())) {
            logger.warn(format("destination stations %s not available at %s and %s ", HasId.asIds(starts), date, timeRange));
            return NumberOfChanges.None();
        }

        Set<Route> startRoutes = pickupRoutesFor(starts, date, timeRange);
        Set<Route> endRoutes = dropoffRoutesFor(destinations, date, timeRange);
        InterchangeOperating interchangesOperating = new InterchangeOperating(date, timeRange);

        if (neighboursRepository.areNeighbours(starts, destinations)) {
            IndexedBitSet dateOverlaps = costs.createOverlapMatrixFor(date);
            int maxHops = maxHops(startRoutes, endRoutes, date, timeRange, interchangesOperating, dateOverlaps);
            return new NumberOfChanges(0, maxHops);
        }
        return getNumberOfHops(startRoutes, endRoutes, date, timeRange, interchangesOperating);
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
        final Set<Route> pickupRoutes = startStation.getPickupRoutes(date, timeRange);
        final Set<Route> dropoffRoutes = destination.getDropoffRoutes(date, timeRange);

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
            return getNumberOfHops(pickupRoutes, dropoffRoutes, date, timeRange, interchangesOperating);
        } else {
            final Set<Route> filteredPickupRoutes = filterForModes(preferredModes, pickupRoutes);
            final Set<Route> filteredDropoffRoutes = filterForModes(preferredModes, dropoffRoutes);

            if (filteredPickupRoutes.isEmpty() || filteredDropoffRoutes.isEmpty()) {
                logger.warn(format("No paths between routes %s and %s due to preferredModes modes %s, filtering gave %s and %s",
                        HasId.asIds(pickupRoutes), HasId.asIds(dropoffRoutes), preferredModes, HasId.asIds(filteredPickupRoutes),
                        HasId.asIds(filteredDropoffRoutes)));
                return NumberOfChanges.None();
            }

            return getNumberOfHops(filteredPickupRoutes, filteredDropoffRoutes, date, timeRange, interchangesOperating);
        }

    }

    @NotNull
    private Set<Route> filterForModes(Set<TransportMode> modes, Set<Route> routes) {
        return routes.stream().filter(route -> modes.contains(route.getTransportMode())).collect(Collectors.toSet());
    }

    @Override
    public LowestCostsForDestRoutes getLowestCostCalcutatorFor(LocationSet destinations, LocalDate date, TimeRange timeRange) {
        Set<Route> destinationRoutes = destinations.stream().
                map(dest -> dest.getDropoffRoutes(date, timeRange)).
                flatMap(Collection::stream).
                collect(Collectors.toUnmodifiableSet());
        return new LowestCostForDestinations(this, destinationRoutes, date, timeRange);
    }

    @NotNull
    private NumberOfChanges getNumberOfHops(Set<Route> startRoutes, Set<Route> destinationRoutes, LocalDate date,
                                            TimeRange timeRange, InterchangeOperating interchangesOperating) {

        IndexedBitSet dateOverlaps = costs.createOverlapMatrixFor(date);

        int minHops = minHops(startRoutes, destinationRoutes, date, timeRange, interchangesOperating, dateOverlaps);
        if (minHops > MAX_DEPTH) {
            logger.error(format("Unexpected result for min hops %s greater than max depth %s, for %s to %s",
                    minHops, MAX_DEPTH, HasId.asIds(startRoutes), HasId.asIds(destinationRoutes)));
        }

        int maxHops = maxHops(startRoutes, destinationRoutes, date, timeRange, interchangesOperating, dateOverlaps);
        if (maxHops > MAX_DEPTH) {
            logger.error(format("Unexpected result for max hops %s greater than max depth %s, for %s to %s",
                    maxHops, MAX_DEPTH, HasId.asIds(startRoutes), HasId.asIds(destinationRoutes)));
        }

        NumberOfChanges numberOfChanges = new NumberOfChanges(minHops, maxHops);
        logger.info(format("Computed number of changes from %s to %s on %s as %s",
                HasId.asIds(startRoutes), HasId.asIds(destinationRoutes), date, numberOfChanges));
        return numberOfChanges;
    }

    private int minHops(Set<Route> startRoutes, Set<Route> endRoutes, LocalDate date, TimeRange time,
                        InterchangeOperating interchangeOperating, IndexedBitSet dateOverlaps) {

        final Optional<Integer> query = startRoutes.stream().
                flatMap(startRoute ->
                        endRoutes.stream().map(endRoute ->
                                getNumberChangesFor(startRoute, endRoute, date, time, interchangeOperating, dateOverlaps))).
                min(Integer::compare);

        if (query.isEmpty()) {
            logger.warn(format("No minHops found for %s to %s", HasId.asIds(startRoutes), HasId.asIds(endRoutes)));
        }
        return query.orElse(Integer.MAX_VALUE);
    }

    private Integer maxHops(Set<Route> startRoutes, Set<Route> endRoutes, LocalDate date, TimeRange time, InterchangeOperating interchangesOperating, IndexedBitSet dateOverlaps) {
        final Optional<Integer> query = startRoutes.stream().
                flatMap(startRoute ->
                        endRoutes.stream().map(endRoute -> getNumberChangesFor(startRoute, endRoute, date, time, interchangesOperating, dateOverlaps))).
                filter(result -> result != Integer.MAX_VALUE).
                max(Integer::compare);

        if (query.isEmpty()) {
            logger.warn(format("No maxHops found for %s to %s", HasId.asIds(startRoutes), HasId.asIds(endRoutes)));
        }
        return query.orElse(Integer.MAX_VALUE);
    }

    private Set<Route> dropoffRoutesFor(LocationSet locations, LocalDate date, TimeRange timeRange) {
        return locations.stream().flatMap(station -> station.getDropoffRoutes(date, timeRange).stream()).collect(Collectors.toSet());
    }

    private Set<Route> pickupRoutesFor(LocationSet locations, LocalDate date, TimeRange timeRange) {
        return locations.stream().flatMap(station -> station.getPickupRoutes(date, timeRange).stream()).collect(Collectors.toSet());
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
                    map(indexOfDest -> routeToRouteCosts.costs.getDepth(RouteIndexPair.of(indexOfStart, indexOfDest), date, time, interchangeOperating,
                            dateOverlaps)).
                    filter(result -> result != Costs.MAX_VALUE).
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
                    map(dest -> routeToRouteCosts.costs.getDepth(RouteIndexPair.of(indexOfStart, dest), date, time, interchangeOperating,
                            dateOverlaps)).
                    min().
                    orElse(Integer.MAX_VALUE);
            return Pair.of(result, start);
        }

    }

    private static class RouteIndex implements DataCache.Cacheable<RouteIndexData> {
        private final RouteRepository routeRepository;
        private final Map<IdFor<Route>, Integer> mapRouteIdToIndex;
        private final Map<Integer, IdFor<Route>> mapIndexToRouteId;
        private final int numberOfRoutes;

        private RouteIndex(RouteRepository routeRepository) {
            this.routeRepository = routeRepository;
            this.numberOfRoutes = routeRepository.numberOfRoutes();
            mapRouteIdToIndex = new HashMap<>(numberOfRoutes);
            mapIndexToRouteId = new HashMap<>(numberOfRoutes);
        }

        public void populateFrom(RouteRepository routeRepository) {
            logger.info("Creating index");
            List<IdFor<Route>> routesList = routeRepository.getRoutes().stream().map(Route::getId).collect(Collectors.toList());
            createIndex(routesList);
            logger.info("Added " + mapRouteIdToIndex.size() + " index entries");
        }

        private void createIndex(List<IdFor<Route>> routesList) {
            for (int i = 0; i < routesList.size(); i++) {
                mapRouteIdToIndex.put(routesList.get(i), i);
                mapIndexToRouteId.put(i, routesList.get(i));
            }
        }

        public int indexFor(IdFor<Route> from) {
            return mapRouteIdToIndex.get(from);
        }

        @Override
        public void cacheTo(DataSaver<RouteIndexData> saver) {
            List<RouteIndexData> indexData = mapRouteIdToIndex.entrySet().stream().
                    map(entry -> new RouteIndexData(entry.getValue(), entry.getKey())).
                    collect(Collectors.toList());
            saver.save(indexData);
            indexData.clear();
        }

        @Override
        public String getFilename() {
            return INDEX_FILE;
        }

        @Override
        public void loadFrom(Stream<RouteIndexData> stream) throws DataCache.CacheLoadException {
            logger.info("Loading from cache");
            stream.forEach(item -> {
                mapRouteIdToIndex.put(item.getRouteId(), item.getIndex());
                mapIndexToRouteId.put(item.getIndex(), item.getRouteId());
            });
            if (mapRouteIdToIndex.size() != numberOfRoutes) {
                throw new DataCache.CacheLoadException("Mismatch on number of routes, got " + mapRouteIdToIndex.size() +
                        " expected " + numberOfRoutes);
            }
        }

        public void clear() {
            mapRouteIdToIndex.clear();
            mapIndexToRouteId.clear();
        }

        public Route getRouteFor(int index) {
            return routeRepository.getRouteById(mapIndexToRouteId.get(index));
        }

        public RoutePair getPairFor(RouteIndexPair indexPair) {
            IdFor<Route> firstId = mapIndexToRouteId.get(indexPair.first);
            IdFor<Route> secondId = mapIndexToRouteId.get(indexPair.second);

            Route first = routeRepository.getRouteById(firstId);
            Route second = routeRepository.getRouteById(secondId);

            return new RoutePair(first, second);
        }
    }

    private static class Costs {
        public static final byte MAX_VALUE = Byte.MAX_VALUE;

        // map from route->route pair to interchanges that could make that change
        private final Map<RouteIndexPair, Set<Station>> routePairToInterchange;
        private final StationAvailabilityRepository availabilityRepository;
        private HashSet<InterchangeStation> interchanges;
        private final IndexedBitSet[] costsForDegree;
        private final RouteIndex index;
        private final int maxDepth;
        private final int numRoutes;

        private Costs(StationAvailabilityRepository availabilityRepository, RouteIndex index, int numRoutes, int maxDepth) {
            this.availabilityRepository = availabilityRepository;
            this.index = index;
            this.maxDepth = maxDepth;
            costsForDegree = new IndexedBitSet[maxDepth];
            routePairToInterchange = new HashMap<>();
            interchanges = new HashSet<>();
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
            interchanges.add(interchange);
        }

        // create a bitmask for route->route changes that are possible on a given date
        private IndexedBitSet createOverlapMatrixFor(LocalDate date) {
            IndexedBitSet matrix = new IndexedBitSet(numRoutes);
            for (int firstRouteIndex = 0; firstRouteIndex < numRoutes; firstRouteIndex++) {
                Route startRoute = index.getRouteFor(firstRouteIndex);
                BitSet result = new BitSet(numRoutes);
                if (startRoute.isAvailableOn(date)) {
                    for(int secondRouteIndex = 0; secondRouteIndex < numRoutes; secondRouteIndex++) {
                        Route endRoute = index.getRouteFor(secondRouteIndex);
                        if (endRoute.isAvailableOn(date)) {
                            result.set(secondRouteIndex);
                        }
                    }
                }
                matrix.insert(firstRouteIndex, result);
            }
            return matrix;
        }

        public int size() {
            int result = 0;
            for (int i = 1; i < maxDepth; i++) {
                result = result + costsForDegree[i].numberOfConnections();
            }
            return result;
        }

        private int getDepth(RouteIndexPair routePair, LocalDate date, TimeRange time,
                             InterchangeOperating interchangeOperating, IndexedBitSet dateOverlaps) {

            List<List<RouteAndInterchanges>> changes = getChangesFor(routePair, dateOverlaps);

            logger.info(format("Found %s changes combinations for %s %s %s", changes.size(), date, time, routePair));

            List<List<RouteAndInterchanges>> filteredByAvailability = changes.stream().
                    filter(items -> interchangeOperating.isOperating(availabilityRepository, items)).
                    collect(Collectors.toList());

            if (changes.size()!=filteredByAvailability.size()) {
                logger.debug(format("Filtered from %s to %s", changes.size(), filteredByAvailability.size()));
            } else {
                logger.debug("Retained " + filteredByAvailability.size());
            }

            Optional<Integer> result = filteredByAvailability.stream().
                    map(List::size).
                    min(Integer::compare);

            return result.orElse(Integer.MAX_VALUE);

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

        public IndexedBitSet costsFor(int currentDegree) {
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
        private List<List<RouteAndInterchanges>> getChangesFor(final RouteIndexPair routePair, IndexedBitSet dateOverlaps) {

            final byte initialDepth = getDegree(routePair);

            if (initialDepth == 0) {
                //logger.warn(format("getChangesFor: No changes needed indexes %s", routePair));
                return Collections.emptyList();
            }
            if (initialDepth == Byte.MAX_VALUE) {
                logger.debug(format("getChangesFor: no changes possible indexes %s", routePair));
                return Collections.emptyList();
            }

            logger.debug(format("Expand for %s initial depth %s", routePair, initialDepth));

            final Set<List<RouteIndexPair>> routeInterchanges = expand(Collections.singletonList(routePair), initialDepth, dateOverlaps);

            logger.debug(format("Got %s set of changes for %s: %s", routeInterchanges.size(), routePair, routeInterchanges));

            List<List<RouteAndInterchanges>> results = routeInterchanges.stream().
                    map(list -> list.stream().map(this::getInterchangeFor).filter(Objects::nonNull)).
                    map(onePossibleSetOfChange -> onePossibleSetOfChange.collect(Collectors.toList()))
                    .collect(Collectors.toList());

            if (logger.isDebugEnabled()) {
                // toString here is expensive
                logger.debug(format("Map %s => %s", routeInterchanges, results));
            }

            return results;
        }

        private Set<List<RouteIndexPair>> expand(List<RouteIndexPair> pairs, int degree, IndexedBitSet dateOverlaps) {
            if (degree == 1) {
                // at degree one we are at direct connections between routes via an interchange so the result is those pairs
                logger.debug("degree 1, expand pair to: " + pairs);
                return Collections.singleton(pairs);
            }

            final int nextDegree = degree - 1;
            final IndexedBitSet overlapsAtDegree = this.costsForDegree[nextDegree]; //.and(dateOverlaps);

            Set<List<RouteIndexPair>> resultsOfExpanion = new HashSet<>();

            // for >1 result is the set o paris where each of the supplied pairs overlaps
            pairs.forEach(pair -> {
                List<Integer> overlapsForPair = getIndexOverlapsFor(overlapsAtDegree, pair);
                overlapsForPair.forEach(overlapForPair -> {
                    if (dateOverlaps.isSet(pair.first, pair.second)) {
                        List<RouteIndexPair> toExpand = formNewRoutePairs(pair, overlapForPair);
                        Set<List<RouteIndexPair>> expansionForPair = expand(toExpand, nextDegree, dateOverlaps);
                        List<RouteIndexPair> resultsForPair = expansionForPair.stream().flatMap(Collection::stream).collect(Collectors.toList());
                        resultsOfExpanion.add(resultsForPair);
                    }
                });
            });

            logger.debug(format("Result of expanding %s => %s", pairs, resultsOfExpanion));

            return resultsOfExpanion;

        }

        @NotNull
        private List<RouteIndexPair> formNewRoutePairs(RouteIndexPair pair, Integer overlapForPair) {
            RouteIndexPair newFirstPair = RouteIndexPair.of(pair.first, overlapForPair);
            RouteIndexPair newSecondPair = RouteIndexPair.of(overlapForPair, pair.second);
            return Arrays.asList(newFirstPair, newSecondPair);
        }

        private RouteAndInterchanges getInterchangeFor(RouteIndexPair indexPair) {
            RoutePair routePair = index.getPairFor(indexPair);

            if (routePairToInterchange.containsKey(indexPair)) {
                Set<Station> changes = routePairToInterchange.get(indexPair);
                RouteAndInterchanges routeAndInterchanges = new RouteAndInterchanges(routePair, changes);
                if (logger.isDebugEnabled()) {
                    logger.debug(format("Found changes %s for %s", HasId.asIds(changes), indexPair));
                }
                return routeAndInterchanges;
            }
            logger.debug("Did not find any interchanges for " + routePair);
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

    private static class RouteIndexPair {
        private final int first;
        private final int second;

        private RouteIndexPair(int first, int second) {
            this.first = first;
            this.second = second;
        }

        public static RouteIndexPair getIndexPairFor(RouteIndex index, Route routeA, Route routeB) {
            int a = index.indexFor(routeA.getId());
            int b = index.indexFor(routeB.getId());
            return of(a, b);
        }


        @Override
        public String toString() {
            return "IntPair{" +
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
            return Objects.hash(first, second);
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
}
