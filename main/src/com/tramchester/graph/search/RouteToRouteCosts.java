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
import com.tramchester.graph.filters.GraphFilterActive;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.NeighboursRepository;
import com.tramchester.repository.RouteRepository;
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

    private final Costs costs;
    private final RouteIndex index;
    private final int numberOfRoutes;
    private final GraphFilterActive graphFilter;
    private final DataCache dataCache;

    @Inject
    public RouteToRouteCosts(RouteRepository routeRepository, InterchangeRepository interchangeRepository,
                             NeighboursRepository neighboursRepository, DataCache dataCache, GraphFilterActive graphFilter) {
        this.routeRepository = routeRepository;
        this.interchangeRepository = interchangeRepository;
        this.neighboursRepository = neighboursRepository;

        numberOfRoutes = routeRepository.numberOfRoutes();
        this.graphFilter = graphFilter;
        this.dataCache = dataCache;
        index = new RouteIndex(routeRepository);
        costs = new Costs(index, numberOfRoutes, MAX_DEPTH + 1);
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

        final CostsForDegree currentMatrix = costs.costsFor(currentDegree);
        final CostsForDegree newMatrix = costs.costsFor(nextDegree);

        for (int routeIndex = 0; routeIndex < numberOfRoutes; routeIndex++) {

            ImmutableBitSet currentConnectionsForRoute = currentMatrix.getConnectionsFor(routeIndex);
            BitSet result = new BitSet(numberOfRoutes);
            for (int connectionIndex = 0; connectionIndex < numberOfRoutes; connectionIndex++) {
                if (currentConnectionsForRoute.get(connectionIndex)) {
                    // if current routeIndex is connected to a route, then for next degree include that other routes connections
                    ImmutableBitSet otherRoutesConnections = currentMatrix.getConnectionsFor(connectionIndex);
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
        final CostsForDegree forDegreeOne = costs.costsForDegree[1];
        interchanges.forEach(interchange -> addOverlapsForInterchange(forDegreeOne, interchange, routeDateAndDayOverlap));
        logger.info("Add " + costs.size() + " connections for interchanges");
    }

    private void addOverlapsForInterchange(CostsForDegree forDegreeOne, InterchangeStation interchange,
                                           RouteDateAndDayOverlap routeDateAndDayOverlap) {

        // record interchanges, where we can go from being dropped off (routes) to being picked up (routes)
        final Set<Route> dropOffAtInterchange = interchange.getDropoffRoutes();
        final Set<Route> pickupAtInterchange = interchange.getPickupRoutes();

        for (final Route dropOff : dropOffAtInterchange) {
            final int dropOffIndex = index.indexFor(dropOff.getId());
            //ImmutableBitSet forDropOffRoute = forDegreeOne.getConnectionsFor(dropOffIndex);
            // todo, could use bitset Or and And with DateOverlapMask here
            for (final Route pickup : pickupAtInterchange) {
                if ((!dropOff.equals(pickup)) && pickup.isDateOverlap(dropOff)) {
                    final int pickupIndex = index.indexFor(pickup.getId());
                    //forDropOffRoute.set(pickupIndex);
                    forDegreeOne.set(dropOffIndex, pickupIndex);
                    costs.addInterchangeBetween(dropOffIndex, pickupIndex, interchange);
                }
            }
            // apply dates and days
            forDegreeOne.applyAndTo(dropOffIndex, routeDateAndDayOverlap.overlapsFor(dropOffIndex));
            //forDropOffRoute.and(routeDateAndDayOverlap.overlapsFor(dropOffIndex));
        }
    }

    /***
     * Use methods from BetweenRoutesCostRepository instead of this
     * @param routeA first route
     * @param routeB second route
     * @return number of changes
     */
    public int getFor(Route routeA, Route routeB, LocalDate date) {
        if (routeA.equals(routeB)) {
            return 0;
        }
        if (!routeA.isDateOverlap(routeB)) {
            logger.debug(format("No date overlap between %s and %s", routeA.getId(), routeB.getId()));
            return Integer.MAX_VALUE;
        }

        RouteIndexPair routePair = RouteIndexPair.getFor(index, routeA, routeB);
        final int result = costs.getDepth(routePair, date);

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

    public List<List<RouteInterchanges>> getChangesFor(Route routeA, Route routeB) {
        RoutePair routePair = new RoutePair(routeA, routeB);

        logger.info("Get change stations betweem " + routePair);

        RouteIndexPair indexPair = RouteIndexPair.getFor(index, routeA, routeB);

        List<List<RouteInterchanges>> result = costs.getChangesFor(indexPair);

        if (result.isEmpty()) {
            logger.warn(format("Unable to find changes between %s", routePair));
        }
        return result;

    }

    public int size() {
        return costs.size();
    }

    @Override
    public NumberOfChanges getNumberOfChanges(StationGroup start, StationGroup end, LocalDate date) {
        return getNumberOfChanges(LocationSet.of(start.getContained()), LocationSet.of(end.getContained()), date);
    }

    @Override
    public NumberOfChanges getNumberOfChanges(LocationSet starts, LocationSet destinations, LocalDate date) {
        if (starts.stream().allMatch(station -> station.getPickupRoutes(date).isEmpty())) {
            logger.warn(format("start stations %s have no pick-up routes", HasId.asIds(starts)));
            return NumberOfChanges.None();
        }
        if (destinations.stream().allMatch(station -> station.getDropoffRoutes(date).isEmpty())) {
            logger.warn(format("destination stations %s have no drop-off routes", HasId.asIds(destinations)));
            return NumberOfChanges.None();
        }
        if (neighboursRepository.areNeighbours(starts, destinations)) {
            return new NumberOfChanges(0, maxHops(pickupRoutesFor(starts, date), dropoffRoutesFor(destinations, date), date));
        }
        return getNumberOfHops(pickupRoutesFor(starts, date), dropoffRoutesFor(destinations, date), date);
    }

    @Override
    public NumberOfChanges getNumberOfChanges(Location<?> startStation, Location<?> destination,
                                              Set<TransportMode> preferredModes, LocalDate date) {
        logger.info(format("Compute number of changes between %s and %s using %s on %s",
                startStation.getId(), destination.getId(), preferredModes, date));

        if (neighboursRepository.areNeighbours(startStation, destination)) {
            return new NumberOfChanges(1, 1);
        }

        final Set<Route> pickupRoutes = startStation.getPickupRoutes(date);
        final Set<Route> dropoffRoutes = destination.getDropoffRoutes(date);

        if (pickupRoutes.isEmpty()) {
            logger.warn(format("start station %s has no pick-up routes", startStation));
            return NumberOfChanges.None();
        }
        if (dropoffRoutes.isEmpty()) {
            logger.warn(format("destination station %s has no drop-off routes", destination));
            return NumberOfChanges.None();
        }

        if (preferredModes.isEmpty()) {
            return getNumberOfHops(pickupRoutes, dropoffRoutes, date);
        } else {
            final Set<Route> filteredPickupRoutes = filterForModes(preferredModes, pickupRoutes);
            final Set<Route> filteredDropoffRoutes = filterForModes(preferredModes, dropoffRoutes);

            if (filteredPickupRoutes.isEmpty() || filteredDropoffRoutes.isEmpty()) {
                logger.warn(format("No paths between routes %s and %s due to preferredModes modes %s, filtering gave %s and %s",
                        HasId.asIds(pickupRoutes), HasId.asIds(dropoffRoutes), preferredModes, HasId.asIds(filteredPickupRoutes),
                        HasId.asIds(filteredDropoffRoutes)));
                return NumberOfChanges.None();
            }

            return getNumberOfHops(filteredPickupRoutes, filteredDropoffRoutes, date);
        }

    }

    @NotNull
    private Set<Route> filterForModes(Set<TransportMode> modes, Set<Route> routes) {
        return routes.stream().filter(route -> modes.contains(route.getTransportMode())).collect(Collectors.toSet());
    }

    @Override
    public LowestCostsForDestRoutes getLowestCostCalcutatorFor(LocationSet destinations, LocalDate date) {
        Set<Route> destinationRoutes = destinations.stream().
                map(dest -> dest.getDropoffRoutes(date)).
                flatMap(Collection::stream).
                collect(Collectors.toUnmodifiableSet());
        return new LowestCostForDestinations(this, destinationRoutes, date);
    }

    @NotNull
    private NumberOfChanges getNumberOfHops(Set<Route> startRoutes, Set<Route> destinationRoutes, LocalDate date) {
        int minHops = minHops(startRoutes, destinationRoutes, date);
        if (minHops == Integer.MAX_VALUE) {
            logger.warn(format("No results founds between %s and %s", HasId.asIds(startRoutes), HasId.asIds(destinationRoutes)));
        }

        int maxHops = maxHops(startRoutes, destinationRoutes, date);
        if (maxHops > MAX_DEPTH) {
            logger.error(format("Unexpected result for max hops %s greater than max depth %s, for %s to %s",
                    maxHops, MAX_DEPTH, HasId.asIds(startRoutes), HasId.asIds(destinationRoutes)));
        }
        NumberOfChanges numberOfChanges = new NumberOfChanges(minHops, maxHops);
        logger.info(format("Computed number of changes from %s to %s on %s as %s",
                HasId.asIds(startRoutes), HasId.asIds(destinationRoutes), date, numberOfChanges));
        return numberOfChanges;
    }

    private int minHops(Set<Route> startRoutes, Set<Route> endRoutes, LocalDate date) {
        final Optional<Integer> query = startRoutes.stream().
                flatMap(startRoute -> endRoutes.stream().map(endRoute -> getFor(startRoute, endRoute, date))).
                min(Integer::compare);
        if (query.isEmpty()) {
            logger.warn(format("No minHops found for %s to %s", HasId.asIds(startRoutes), HasId.asIds(endRoutes)));
        }
        return query.orElse(Integer.MAX_VALUE);
    }

    private Integer maxHops(Set<Route> startRoutes, Set<Route> endRoutes, LocalDate date) {
        final Optional<Integer> query = startRoutes.stream().
                flatMap(startRoute -> endRoutes.stream().map(endRoute -> getFor(startRoute, endRoute, date))).
                filter(result -> result != Integer.MAX_VALUE).
                max(Integer::compare);

        if (query.isEmpty()) {
            logger.warn(format("No maxHops found for %s to %s", HasId.asIds(startRoutes), HasId.asIds(endRoutes)));
        }
        return query.orElse(Integer.MAX_VALUE);
    }

    private Set<Route> dropoffRoutesFor(LocationSet locations, LocalDate date) {
        return locations.stream().flatMap(station -> station.getDropoffRoutes(date).stream()).collect(Collectors.toSet());
    }

    private Set<Route> pickupRoutesFor(LocationSet locations, LocalDate date) {
        return locations.stream().flatMap(station -> station.getPickupRoutes(date).stream()).collect(Collectors.toSet());
    }


    private static class LowestCostForDestinations implements LowestCostsForDestRoutes {
        private final RouteToRouteCosts routeToRouteCosts;
        private final Set<Integer> destinationIndexs;
        private final LocalDate date;

        public LowestCostForDestinations(BetweenRoutesCostRepository routeToRouteCosts, Set<Route> destinations, LocalDate date) {
            this.routeToRouteCosts = (RouteToRouteCosts) routeToRouteCosts;
            destinationIndexs = destinations.stream().
                    map(destination -> this.routeToRouteCosts.index.indexFor(destination.getId())).
                    collect(Collectors.toUnmodifiableSet());
            this.date = date;
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
                    map(indexOfDest -> routeToRouteCosts.costs.getDepth(RouteIndexPair.of(indexOfStart, indexOfDest), date)).
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
                    map(dest -> routeToRouteCosts.costs.getDepth(RouteIndexPair.of(indexOfStart, dest), date)).
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

    private static class CostsForDegree {
        private final BitSet[] rows;
        private final int numberOfRoutes;

        private CostsForDegree(int numberOfRoutes) {
            rows = new BitSet[numberOfRoutes];
            this.numberOfRoutes = numberOfRoutes;
            for (int i = 0; i < numberOfRoutes; i++) {
                rows[i] = new BitSet(numberOfRoutes);
            }
        }

        public void set(int indexA, int indexB) {
            rows[indexA].set(indexB);
        }

        public boolean isSet(int indexA, int indexB) {
            return rows[indexA].get(indexB);
        }

        private ImmutableBitSet getConnectionsFor(int routesIndex) {
            return new ImmutableBitSet(rows[routesIndex]);
        }

        public void insert(int routeIndex, BitSet connectionsForRoute) {
            rows[routeIndex] = connectionsForRoute;
        }

        public int numberOfConnections() {
            int count = 0;
            for (int row = 0; row < numberOfRoutes; row++) {
                count = count + rows[row].cardinality();
            }
            return count;
        }

        public void clear() {
            for (int i = 0; i < numberOfRoutes; i++) {
                rows[i].clear();
            }
        }

        public boolean isSet(RouteIndexPair routePair) {
            return isSet(routePair.first, routePair.second);
        }

        public void applyAndTo(int index, BitSet bitSet) {
            BitSet mutable = rows[index];
            mutable.and(bitSet);
        }
    }

    private static class Costs {
        public static final byte MAX_VALUE = Byte.MAX_VALUE;

        private final Map<RouteIndexPair, Set<Station>> interchanges;
        private final CostsForDegree[] costsForDegree;
        private final RouteIndex index;
        private final int maxDepth;

        private Costs(RouteIndex index, int numRoutes, int maxDepth) {
            this.index = index;
            this.maxDepth = maxDepth;
            costsForDegree = new CostsForDegree[maxDepth];
            interchanges = new HashMap<>();
            for (int degree = 1; degree < maxDepth; degree++) {
                costsForDegree[degree] = new CostsForDegree(numRoutes);
            }
        }

        public int size() {
            int result = 0;
            for (int i = 1; i < maxDepth; i++) {
                result = result + costsForDegree[i].numberOfConnections();
            }
            return result;
        }

        public boolean contains(int degree, int routeIndexA, int routeIndexB) {
            return costsForDegree[degree].isSet(routeIndexA, routeIndexB);
        }

        private int getDepth(RouteIndexPair routePair, LocalDate date) {
            List<List<RouteInterchanges>> changes = getChangesFor(routePair);

            Optional<Integer> result = changes.stream().
                    filter(routeInterchanges -> isOperating(routeInterchanges, date)).
                    map(List::size).
                    min(Integer::compare);

            return result.orElse(Integer.MAX_VALUE);

        }

        /***
         *
         * @param changeSet list of route interchanges to get between 2 routes
         * @param date the date to filter for
         * @return true iff any of the list of interchange have at least one path that is valid on the date
         */
        private boolean isOperating(List<RouteInterchanges> changeSet, LocalDate date) {
            return changeSet.stream().allMatch(routeInterchanges -> routeInterchanges.availableOn(date));
        }

        private byte getDegree(RouteIndexPair routePair) {
            if (routePair.isSame()) {
                return 0;
            }
            for (int depth = 1; depth < maxDepth; depth++) {
                if (costsForDegree[depth].isSet(routePair)) {
                    return (byte) depth;
                }
            }
            return MAX_VALUE;
        }

        public CostsForDegree costsFor(int currentDegree) {
            return costsForDegree[currentDegree];
        }

        public void clear() {
            for (int degree = 1; degree < maxDepth; degree++) {
                costsForDegree[degree].clear();
            }
        }

        /***
         * @param routePair indexes for first and second routes
         * @return each set returned contains specific interchanges between 2 specific routes
         */
        public List<List<RouteInterchanges>> getChangesFor(final RouteIndexPair routePair) {

            final byte initialDepth = getDegree(routePair);

            if (initialDepth == 0) {
                //logger.warn(format("getChangesFor: No changes needed indexes %s", routePair));
                return Collections.emptyList();
            }
            if (initialDepth == Byte.MAX_VALUE) {
                logger.warn(format("getChangesFor: no changes possible indexes %s", routePair));
                return Collections.emptyList();
            }

            logger.debug(format("Expand for %s initial depth %s", routePair, initialDepth));

            final Set<List<RouteIndexPair>> routeInterchanges = expand(Collections.singletonList(routePair), initialDepth);

            logger.debug(format("Got %s set of changes for %s: %s", routeInterchanges.size(), routePair, routeInterchanges));

            List<List<RouteInterchanges>> results = routeInterchanges.stream().
                    map(list -> list.stream().map(this::getInterchangeFor).filter(Objects::nonNull)).
                    map(onePossibleSetOfChange -> onePossibleSetOfChange.collect(Collectors.toList()))
                    .collect(Collectors.toList());

            if (logger.isDebugEnabled()) {
                // toString here is expensive
                logger.debug(format("Map %s => %s", routeInterchanges, results));
            }

            return results;
        }

        public Set<List<RouteIndexPair>> expand(List<RouteIndexPair> pairs, int degree) {
            if (degree == 1) {
                logger.debug("degree 1, expand pair to: " + pairs);
                return Collections.singleton(pairs);
            }

            final int nextDegree = degree - 1;
            final CostsForDegree costsForDegree = this.costsForDegree[nextDegree];

            Set<List<RouteIndexPair>> resultsOfExpanion = new HashSet<>();

            pairs.forEach(pair -> {
                List<Integer> overlapsForPair = getIndexOverlapsFor(costsForDegree, pair);
                overlapsForPair.forEach(overlapForPair -> {
                    List<RouteIndexPair> toExpand = formNewRoutePairs(pair, overlapForPair);
                    Set<List<RouteIndexPair>> expansionForPair = expand(toExpand, nextDegree);
                    List<RouteIndexPair> resultsForPair = expansionForPair.stream().flatMap(Collection::stream).collect(Collectors.toList());
                    resultsOfExpanion.add(resultsForPair);
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

        private RouteInterchanges getInterchangeFor(RouteIndexPair indexPair) {
            RoutePair routePair = index.getPairFor(indexPair);

            if (interchanges.containsKey(indexPair)) {
                Set<Station> changes = interchanges.get(indexPair);
                RouteInterchanges routeInterchanges = new RouteInterchanges(routePair, changes);
                logger.debug(format("Found changes %s for %s", HasId.asIds(changes), indexPair));
                return routeInterchanges;
            }
            logger.error("Did not find any interchanges for " + routePair);
            return null;
        }

        private List<Integer> getIndexOverlapsFor(CostsForDegree costsForDegree, RouteIndexPair pair) {
            ImmutableBitSet linksForA = costsForDegree.getConnectionsFor(pair.first);
            ImmutableBitSet linksForB = costsForDegree.getConnectionsFor(pair.second);
            ImmutableBitSet overlap = linksForA.and(linksForB);
            return overlap.stream().boxed().collect(Collectors.toList());
        }

        public void addInterchangeBetween(int dropOffIndex, int pickupIndex, InterchangeStation interchange) {
            RouteIndexPair key = RouteIndexPair.of(dropOffIndex, pickupIndex);
            if (!interchanges.containsKey(key)) {
                interchanges.put(key, new HashSet<>());
            }
            interchanges.get(key).add(interchange.getStation());
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

        public static RouteIndexPair getFor(RouteIndex index, Route routeA, Route routeB) {
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

}
