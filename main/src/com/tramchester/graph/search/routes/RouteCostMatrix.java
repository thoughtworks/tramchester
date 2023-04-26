package com.tramchester.graph.search.routes;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.caching.DataCache;
import com.tramchester.dataexport.DataSaver;
import com.tramchester.dataimport.data.CostsPerDegreeData;
import com.tramchester.domain.Route;
import com.tramchester.domain.RoutePair;
import com.tramchester.domain.collections.*;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.places.InterchangeStation;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.filters.GraphFilterActive;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.NumberOfRoutes;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.String.format;

@LazySingleton
public class RouteCostMatrix implements RouteCostCombinations {
    private static final Logger logger = LoggerFactory.getLogger(RouteCostMatrix.class);

    public static final byte MAX_VALUE = Byte.MAX_VALUE;

    public static final int MAX_DEPTH = 5;

    private final InterchangeRepository interchangeRepository;
    private final DataCache dataCache;
    private final GraphFilterActive graphFilter;
    private final RouteIndexPairFactory pairFactory;

    private final CostsPerDegree costsForDegree;
    private final RouteIndex index;
    private final int maxDepth;
    private final int numRoutes;

    // WIP SPIKE
    private final Map<RouteIndexPair, Set<InterchangeStation>> pairToInterchanges;
    private final List<RouteConnectingLinks> underlyingPairs;

    @Inject
    RouteCostMatrix(NumberOfRoutes numberOfRoutes, InterchangeRepository interchangeRepository, DataCache dataCache,
                    GraphFilterActive graphFilter, RouteIndexPairFactory pairFactory, RouteIndex index) {
        this.interchangeRepository = interchangeRepository;
        this.dataCache = dataCache;
        this.graphFilter = graphFilter;
        this.pairFactory = pairFactory;

        this.index = index;
        this.maxDepth = MAX_DEPTH;
        this.numRoutes = numberOfRoutes.numberOfRoutes();

        costsForDegree = new CostsPerDegree(maxDepth);

        underlyingPairs = new ArrayList<>(MAX_DEPTH-1);

        pairToInterchanges = new HashMap<>();

    }

    @PostConstruct
    private void start() {

        if (graphFilter.isActive()) {
            logger.warn("Filtering is enabled, skipping all caching");
            createCostMatrix();
        } else {
            if (dataCache.has(costsForDegree)) {
                logger.info("Loading from cache");
                dataCache.loadInto(costsForDegree, CostsPerDegreeData.class);
            } else {
                logger.info("Not in cache, creating");
                createCostMatrix();
                dataCache.save(costsForDegree, CostsPerDegreeData.class);
            }
        }

        createBacktracking();
        recordPairsToInterchanges();

    }

    private void recordPairsToInterchanges() {
        final Set<InterchangeStation> interchanges = interchangeRepository.getAllInterchanges();
        logger.info("Capture route index paris to interchanges for " + interchanges.size() + " interchanges");

        interchanges.forEach(interchange -> {

            final Set<Route> dropOffAtInterchange = interchange.getDropoffRoutes();
            final Set<Route> pickupAtInterchange = interchange.getPickupRoutes();

            final Set<RouteIndexPair> pairsForInterchange = new HashSet<>();

            for (final Route dropOff : dropOffAtInterchange) {
                final short dropOffIndex = index.indexFor(dropOff.getId());
                  for (final Route pickup : pickupAtInterchange) {
                      if ((!dropOff.equals(pickup)) && pickup.isDateOverlap(dropOff)) {
                          final short pickupIndex = index.indexFor(pickup.getId());
                          pairsForInterchange.add(pairFactory.get(dropOffIndex, pickupIndex));
                      }
                }
            }

            addRoutePairsForInterchange(pairToInterchanges, interchange, pairsForInterchange);
        });

        logger.info("Add " + size() + " paris for interchanges");

    }

    private void createBacktracking() {
        for (int depth = 0; depth < MAX_DEPTH - 1; depth++) {
            underlyingPairs.add(new RouteConnectingLinks(pairFactory, numRoutes, index));
        }
        for (byte currentDegree = 1; currentDegree < maxDepth; currentDegree++) {
            createBacktracking(currentDegree);
        }
    }

    private void createBacktracking(final int currentDegree) {
        final int totalSize = numRoutes * numRoutes;
        if (currentDegree<1) {
            throw new RuntimeException("Only call for >1 , got " + currentDegree);
        }
        logger.info("Create backtrack pair map for degree " + currentDegree);

        final int nextDegree = currentDegree + 1;
        final IndexedBitSet matrixForDegree = costsForDegree.getDegree(currentDegree);

        if (matrixForDegree.numberOfBitsSet()>0) {
            // zero indexed
            final RouteConnectingLinks connectingLinks = underlyingPairs.get(nextDegree - 2); // zero indexed

            final Instant startTime = Instant.now();

            for (short route = 0; route < numRoutes; route++) {
                final short routeIndex = route;

                final ImmutableBitSet currentConnectionsForRoute = matrixForDegree.getBitSetForRow(routeIndex);

                currentConnectionsForRoute.getBitIndexes().forEach(connectedRoute -> {
                    final ImmutableBitSet connections = matrixForDegree.getBitSetForRow(connectedRoute);
                    connectingLinks.addLinksBetween(routeIndex, (short) connectedRoute, connections);
                });
            }

            final long took = Duration.between(startTime, Instant.now()).toMillis();
            int added = connectingLinks.size();
            double percentage = ((double)added)/((double)totalSize) * 100D;
            logger.info(String.format("Added backtrack paris %s (%s %%) Degree %s in %s ms",
                    added, percentage, nextDegree, took));

        } else {
            logger.info("No bits set for degree " + currentDegree);
        }
    }

    private void createCostMatrix() {
        RouteDateAndDayOverlap routeDateAndDayOverlap = new RouteDateAndDayOverlap(index, numRoutes);
        routeDateAndDayOverlap.populateFor();

        IndexedBitSet forDegreeOne = costsForDegree.getDegree(1);
        addInitialConnectionsFromInterchanges(routeDateAndDayOverlap, forDegreeOne);

        populateCosts(routeDateAndDayOverlap);
    }

    @PreDestroy
    private void clear() {
        costsForDegree.clear();
    }

    private void addInitialConnectionsFromInterchanges(RouteDateAndDayOverlap routeDateAndDayOverlap, IndexedBitSet forDegreeOne) {
        final Set<InterchangeStation> interchanges = interchangeRepository.getAllInterchanges();
        logger.info("Pre-populate route to route costs from " + interchanges.size() + " interchanges");

        interchanges.forEach(interchange -> {
            // TODO This does not work for multi-mode station interchanges?
            // record interchanges, where we can go from being dropped off (routes) to being picked up (routes)
            final Set<Route> dropOffAtInterchange = interchange.getDropoffRoutes();
            final Set<Route> pickupAtInterchange = interchange.getPickupRoutes();

           addOverlapsForRoutes(forDegreeOne, routeDateAndDayOverlap, dropOffAtInterchange, pickupAtInterchange);
        });
        logger.info("Add " + size() + " connections for interchanges");
    }

    private void addRoutePairsForInterchange(Map<RouteIndexPair, Set<InterchangeStation>> pairToInterchanges, InterchangeStation interchange,
                                             Set<RouteIndexPair> pairs) {
        pairs.forEach(pair -> {
            if (!pairToInterchanges.containsKey(pair)) {
                pairToInterchanges.put(pair, new HashSet<>());
            }
            pairToInterchanges.get(pair).add(interchange);
        });
    }

    private void addOverlapsForRoutes(IndexedBitSet forDegreeOne, RouteDateAndDayOverlap routeDateAndDayOverlap,
                                      Set<Route> dropOffAtInterchange, Set<Route> pickupAtInterchange) {
        for (final Route dropOff : dropOffAtInterchange) {
            final int dropOffIndex = index.indexFor(dropOff.getId());
            // todo, could use bitset Or and And with DateOverlapMask here
            for (final Route pickup : pickupAtInterchange) {
                if ((!dropOff.equals(pickup)) && pickup.isDateOverlap(dropOff)) {
                    final int pickupIndex = index.indexFor(pickup.getId());
                    forDegreeOne.set(dropOffIndex, pickupIndex);
                }
            }
            // apply dates and days
            forDegreeOne.applyAndTo(dropOffIndex, routeDateAndDayOverlap.overlapsFor(dropOffIndex));
        }
    }

    // create a bitmask for route->route changes that are possible on a given date and transport mode
    @Override
    public IndexedBitSet createOverlapMatrixFor(TramDate date, Set<TransportMode> requestedModes) {
        final Set<Short> availableOnDate = new HashSet<>();
        for (short routeIndex = 0; routeIndex < numRoutes; routeIndex++) {
            final Route route = index.getRouteFor(routeIndex);
            if (route.isAvailableOn(date) && requestedModes.contains(route.getTransportMode())) {
                availableOnDate.add(routeIndex);
            }
        }

        IndexedBitSet result = IndexedBitSet.Square(numRoutes);
        for (short firstRouteIndex = 0; firstRouteIndex < numRoutes; firstRouteIndex++) {
            SimpleBitmap row = SimpleBitmap.create(numRoutes);
            if (availableOnDate.contains(firstRouteIndex)) {
                for (short secondRouteIndex = 0; secondRouteIndex < numRoutes; secondRouteIndex++) {
                    if (availableOnDate.contains(secondRouteIndex)) {
                        row.set(secondRouteIndex);
                    }
                }
            }
            result.insert(firstRouteIndex, row);
        }
        availableOnDate.clear();

        logger.info(format("created overlap matrix for %s and modes %s with %s entries", date, requestedModes, result.numberOfBitsSet()));
        return result;
    }

    @Override
    public long size() {
        return costsForDegree.size();
    }

    private byte getDegree(final RouteIndexPair routePair) {
        if (routePair.isSame()) {
            return 0;
        }
        for (int depth = 1; depth <= maxDepth; depth++) {
            if (costsForDegree.isSet(depth, routePair)) {
                return (byte) depth;
            }
        }
        return MAX_VALUE;
    }

    /***
     * Test support, get all degrees where pair is found
     * @param routeIndexPair pair to fetch all degrees for
     * @return list of matches
     */
    public List<Integer> getAllDegrees(final RouteIndexPair routeIndexPair) {
        if (routeIndexPair.isSame()) {
            throw new RuntimeException("Don't call with same routes");
        }
        final List<Integer> results = new ArrayList<>();
        for (int depth = 1; depth <= maxDepth; depth++) {
            if (costsForDegree.isSet(depth, routeIndexPair)) {
                results.add(depth);
            }
        }
        return results;
    }

    @Override
    public int getMaxDepth() {
        return MAX_DEPTH;
    }

    @Override
    public int getDepth(RouteIndexPair routePair) {
        return getDegree(routePair);
    }

    private boolean isOverlap(final IndexedBitSet bitSet, final RouteIndexPair pair) {
        return bitSet.isSet(pair);
    }

    private void populateCosts(RouteDateAndDayOverlap routeDateAndDayOverlap) {
        final int size = numRoutes;
        final int fullyConnected = size * size;

        logger.info("Find costs between " + size + " routes (" + fullyConnected + ")");

        long previousTotal = 0;
        for (byte currentDegree = 1; currentDegree < maxDepth; currentDegree++) {
            addConnectionsFor(routeDateAndDayOverlap, currentDegree);
            final long currentTotal = size();
            logger.info("Total number of connections " + currentTotal);
            if (currentTotal >= fullyConnected) {
                break;
            }
            if (previousTotal==currentTotal) {
                logger.warn(format("No improvement in connections at depth %s and number %s", currentDegree, currentTotal));
                break;
            }
            previousTotal = currentTotal;
        }

        final long finalSize = size();
        logger.info("Added cost for " + finalSize + " route combinations");
        if (finalSize < fullyConnected) {
            double percentage = ((double) finalSize / (double) fullyConnected);
            logger.warn(format("Not fully connected, only %s (%s) of %s ", finalSize, percentage, fullyConnected));
        } else {
            logger.info(format("Fully connected, with %s of %s ", finalSize, fullyConnected));
        }
    }

    // based on the previous degree and connections, add further connections at current degree which are
    // enabled by the previous degree. For example if degree 1 has: R1->R2 at IntA and R2->R3 at IntB then
    // at degree 2 we have: R1->R3
    // implementation uses a bitmap to do this computation quickly row by row
    private void addConnectionsFor(RouteDateAndDayOverlap routeDateAndDayOverlap, byte currentDegree) {
        final Instant startTime = Instant.now();
        final int nextDegree = currentDegree + 1;

        final IndexedBitSet currentMatrix = costsForDegree.getDegree(currentDegree);
        final IndexedBitSet newMatrix = costsForDegree.getDegree(nextDegree);

        for (int route = 0; route < numRoutes; route++) {
            final SimpleBitmap resultForForRoute = SimpleBitmap.create(numRoutes);
            final ImmutableBitSet currentConnectionsForRoute = currentMatrix.getBitSetForRow(route);

            currentConnectionsForRoute.getBitIndexes().forEach(connectedRoute -> {
                // if current route is connected to another route, then for next degree include that other route's connections
                final ImmutableBitSet otherRoutesConnections = currentMatrix.getBitSetForRow(connectedRoute);
                otherRoutesConnections.applyOrTo(resultForForRoute);
            });

            final SimpleBitmap dateOverlapMask = routeDateAndDayOverlap.overlapsFor(route);  // only those routes whose dates overlap
            resultForForRoute.and(dateOverlapMask);

            final ImmutableBitSet allExistingConnectionsForRoute = getExistingBitSetsForRoute(route, currentDegree);

            allExistingConnectionsForRoute.applyAndNotTo(resultForForRoute);  // don't include any current connections for this route

            newMatrix.insert(route, resultForForRoute);
        }

        final long took = Duration.between(startTime, Instant.now()).toMillis();
        logger.info("Added " + newMatrix.numberOfBitsSet() + " connections for  degree " + nextDegree + " in " + took + " ms");
    }

    public ImmutableBitSet getExistingBitSetsForRoute(final int routeIndex, final int startingDegree) {
        final IndexedBitSet connectionsAtAllDepths = new IndexedBitSet(1, numRoutes);

        for (int degree = startingDegree; degree > 0; degree--) {
            IndexedBitSet allConnectionsAtDegree = costsForDegree.getDegree(degree);
            ImmutableBitSet existingConnectionsAtDepth = allConnectionsAtDegree.getBitSetForRow(routeIndex);
            connectionsAtAllDepths.or(existingConnectionsAtDepth);
        }

        return connectionsAtAllDepths.createImmutable();
    }

    public int getConnectionDepthFor(Route routeA, Route routeB) {
        RouteIndexPair routePair = index.getPairFor(RoutePair.of(routeA, routeB));
        return getDegree(routePair);
    }

    public AnyOfPaths getInterchangesFor(final RouteIndexPair indexPair, final IndexedBitSet dateOverlaps) {
        final int degree = getDepth(indexPair);

        final IndexedBitSet changesForDegree = costsForDegree.getDegree(degree).getRowAndColumn(indexPair.firstAsInt(), indexPair.secondAsInt());
        // apply mask to filter out unavailable dates/modes
        final IndexedBitSet withDateApplied = changesForDegree.and(dateOverlaps);

        if (withDateApplied.isSet(indexPair)) {

            return getPathFor(indexPair, degree, dateOverlaps);

        } else {
            return new AnyOfContained();
        }
    }

    private AnyOfPaths getPathFor(final RouteIndexPair indexPair, final int degree, final IndexedBitSet dateOverlaps) {
        final IndexedBitSet changesForDegree = costsForDegree.getDegree(degree).getRowAndColumn(indexPair.firstAsInt(), indexPair.secondAsInt());
        // apply mask to filter out unavailable dates/modes
        final IndexedBitSet withDateApplied = changesForDegree.and(dateOverlaps);

        if (withDateApplied.isSet(indexPair)) {

            if (degree==1) {
                if (!pairToInterchanges.containsKey(indexPair)) {
                    RoutePair routePair = index.getPairFor(indexPair);
                    String msg = "Unable to find interchange for " + HasId.asIds(routePair) + " at degree " + degree;
                    logger.error(msg);
                    throw new RuntimeException(msg);
                }
                final Set<InterchangeStation> changes = pairToInterchanges.get(indexPair);
                return new AnyOfInterchanges(changes);
            } else {
                //int previousPairsIndex = 0;
                final int depth = degree - 1;
                final AnyOfContained result = new AnyOfContained();
                final Set<Pair<RouteIndexPair, RouteIndexPair>> underlying = underlyingPairs.get(depth-1).getLinksFor(indexPair);

                underlying.forEach(pair -> {
                    final AnyOfPaths pathA = getPathFor(pair.getLeft(), degree - 1, dateOverlaps);
                    final AnyOfPaths pathB = getPathFor(pair.getRight(), degree - 1, dateOverlaps);
                    result.add(new BothOfPaths(pathA, pathB));
                });

                return result;

            }

        } else {
            return new AnyOfContained();
        }
    }

    private static class RouteDateAndDayOverlap {
        // Create a bitmask corresponding to the dates and days routes overlap
        // NOTE: this is for route overlaps only, it does cover whether specific stations
        // are served by the routes on a specific date

        private final SimpleBitmap[] overlapMasks;
        private final int numberOfRoutes;
        private final RouteIndex index;

        private RouteDateAndDayOverlap(RouteIndex index, int numberOfRoutes) {
            this.index = index;
            overlapMasks = new SimpleBitmap[numberOfRoutes];
            this.numberOfRoutes = numberOfRoutes;
        }

        public void populateFor() {
            logger.info("Creating matrix for route date/day overlap");

            for (short i = 0; i < numberOfRoutes; i++) {
                final Route from = index.getRouteFor(i);
                SimpleBitmap resultsForRoute = SimpleBitmap.create(numberOfRoutes);
                final int fromIndex = i;
                // thread safety: split into list and then application of list to bitset
                List<Integer> toSet = IntStream.range(0, numberOfRoutes).
                        parallel().
                        filter(toIndex -> (fromIndex == toIndex) || from.isDateOverlap(index.getRouteFor((short)toIndex))).
                        boxed().collect(Collectors.toList());
                toSet.forEach(resultsForRoute::set);
                overlapMasks[i] = resultsForRoute;
            }
            logger.info("Finished matrix for route date/day overlap");
        }

        public SimpleBitmap overlapsFor(int routeIndex) {
            return overlapMasks[routeIndex];
        }
    }

    private static class RouteConnectingLinks {
        private final RouteIndexPairFactory pairFactory;

        // (A, C) -> (A, B) (B ,C)
        // reduces to => (A,C) -> B

        // pair to connecting route index (A,B) -> [C]
        private final Map<Integer, BitSet> bitSetForIndex;
        private final int numRoutes;
        private final RouteIndex index; // diagnostics

        private final BitmapAsRoaringBitmap seen; // performance

        private RouteConnectingLinks(RouteIndexPairFactory pairFactory, int numRoutes, RouteIndex index) {
            this.pairFactory = pairFactory;
            this.numRoutes = numRoutes;
            this.index = index;
            bitSetForIndex = new HashMap<>();
            seen = new BitmapAsRoaringBitmap(numRoutes * numRoutes);
        }

        public void addLinksBetween(final short routeIndexA, final short routeIndexB, final ImmutableBitSet links) {
            links.getBitIndexes().
                    mapToObj(linkIndex -> pairFactory.get(routeIndexA, (short)linkIndex)).
                    map(this::getBitSetForPair).
                    forEach(bitSet -> bitSet.set(routeIndexB));
        }

        private BitSet getBitSetForPair(final RouteIndexPair pair) {
            final int position = getPositionFor(pair);

            if (seen.get(position)) {
                return bitSetForIndex.get(position);
            }

            final BitSet bitSet = new BitSet();
            bitSetForIndex.put(position, bitSet);
            seen.set(position);
            return bitSet;
        }

        private int getPositionFor(RouteIndexPair routeIndexPair) {
            return (routeIndexPair.firstAsInt()*numRoutes) + routeIndexPair.secondAsInt();
        }

        // re-expand from (A,C) -> B into: (A,B) (B,C)
        public Set<Pair<RouteIndexPair, RouteIndexPair>> getLinksFor(RouteIndexPair indexPair) {
            final int position = getPositionFor(indexPair);
            if (!seen.get(position)) {
                RoutePair missing = index.getPairFor(indexPair);
                String message = "Missing indexPair " + indexPair + " (" + missing + ") in map size " + bitSetForIndex.size();
                logger.error(message);
                throw new RuntimeException(message);
            }

            BitSet connectingRoutes = bitSetForIndex.get(position);
            return connectingRoutes.stream().
                    mapToObj(link -> (short) link).
                    map(link -> Pair.of(pairFactory.get(indexPair.first(), link), pairFactory.get(link, indexPair.second()))).
                    collect(Collectors.toSet());
        }

        public int size() {
            return bitSetForIndex.size();
        }

    }

    public interface InterchangePath {
        boolean isValid(Function<InterchangeStation, Boolean> validator);
        InterchangePath filter(Function<InterchangeStation, Boolean> filter);
        boolean hasAny();

        int getDepth();
    }

    public interface AnyOfPaths extends InterchangePath {
        int numberPossible();

        Stream<InterchangePath> stream();
    }

    public static class AnyOfContained implements AnyOfPaths {
        private final Set<InterchangePath> paths;

        private AnyOfContained(Set<InterchangePath> paths) {
            this.paths = paths;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AnyOfContained that = (AnyOfContained) o;
            return paths.equals(that.paths);
        }

        @Override
        public int hashCode() {
            return Objects.hash(paths);
        }

        public AnyOfContained() {
            paths = new HashSet<>();
        }

        public void add(BothOfPaths bothOfPaths) {
            paths.add(bothOfPaths);
        }

        @Override
        public int numberPossible() {
            return paths.size();
        }

        @Override
        public Stream<InterchangePath> stream() {
            return paths.stream();
        }

        @Override
        public String toString() {
            return "AnyOfContained{" + toString(paths) +
                    '}';
        }

        private String toString(Set<InterchangePath> paths) {
            StringBuilder output = new StringBuilder();
            paths.forEach(interchangePath -> {
                output.append(System.lineSeparator());
                output.append(interchangePath.toString());
            });
            output.append(System.lineSeparator());
            return output.toString();
        }

        @Override
        public boolean isValid(Function<InterchangeStation, Boolean> validator) {
            return paths.stream().anyMatch(path -> path.isValid(validator));
        }

        @Override
        public InterchangePath filter(Function<InterchangeStation, Boolean> filter) {
            Set<InterchangePath> matching = paths.stream().map(path -> path.filter(filter)).collect(Collectors.toSet());
            return new AnyOfContained(matching);
        }

        @Override
        public boolean hasAny() {
            return paths.stream().anyMatch(InterchangePath::hasAny);
        }

        @Override
        public int getDepth() {
            Optional<Integer> anyMatch = paths.stream().map(InterchangePath::getDepth).max(Integer::compareTo);
            return anyMatch.orElse(Integer.MAX_VALUE);
        }
    }


    public static class AnyOfInterchanges implements AnyOfPaths {
        // any of these changes being available makes the path valid
        private final Set<InterchangeStation> changes;

        public AnyOfInterchanges(Set<InterchangeStation> changes) {
            if (changes==null) {
                throw new RuntimeException("Cannot pass in null changes");
            }
            this.changes = changes;
        }

        @Override
        public String toString() {
            return "AnyOfInterchanges{" +
                    "changes=" + changes +
                    '}';
        }

        @Override
        public int numberPossible() {
            return changes.size();
        }

        @Override
        public Stream<InterchangePath> stream() {
            return changes.stream().map(change -> new AnyOfInterchanges(Collections.singleton(change)));
        }

        @Override
        public AnyOfPaths filter(Function<InterchangeStation, Boolean> filter) {
            if (changes.isEmpty()) {
                return new AnyOfInterchanges(Collections.emptySet());
            }
            return new AnyOfInterchanges(changes.stream().filter(filter::apply).collect(Collectors.toSet()));
        }

        @Override
        public boolean hasAny() {
            return !changes.isEmpty();
        }

        @Override
        public int getDepth() {
            // TODO number of changes when empty?
            return 1;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AnyOfInterchanges that = (AnyOfInterchanges) o;
            return changes.equals(that.changes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(changes);
        }

        @Override
        public boolean isValid(Function<InterchangeStation, Boolean> validator) {
            return changes.stream().anyMatch(validator::apply);
        }
    }

    public static class BothOfPaths implements InterchangePath {
        private final InterchangePath pathsA;
        private final InterchangePath pathsB;

        public BothOfPaths(InterchangePath pathsA, InterchangePath pathsB) {
            this.pathsA = pathsA;
            this.pathsB = pathsB;
        }

        @Override
        public String toString() {
            return "BothOfPaths{" +
                    "pathsA=" + pathsA +
                    ", pathsB=" + pathsB +
                    '}';
        }

        @Override
        public boolean isValid(Function<InterchangeStation, Boolean> validator) {
            return pathsA.isValid(validator) && pathsB.isValid(validator);
        }

        @Override
        public InterchangePath filter(Function<InterchangeStation, Boolean> filter) {
            return new BothOfPaths(pathsA.filter(filter), pathsB.filter(filter));
        }

        @Override
        public boolean hasAny() {
            return pathsA.hasAny() && pathsB.hasAny();
        }

        @Override
        public int getDepth() {
            int contained = Math.max(pathsA.getDepth(), pathsB.getDepth());
            return contained + 1;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BothOfPaths that = (BothOfPaths) o;
            return pathsA.equals(that.pathsA) && pathsB.equals(that.pathsB);
        }

        @Override
        public int hashCode() {
            return Objects.hash(pathsA, pathsB);
        }

        public InterchangePath getFirst() {
            return pathsA;
        }

        public InterchangePath getSecond() {
            return pathsB;
        }
    }

    /***
     * encapsulate cost per degree to facilitate caching
     */
    private class CostsPerDegree implements DataCache.CachesData<CostsPerDegreeData> {
        private final IndexedBitSet[] bitSets;
        private final int size;

        private CostsPerDegree(int size) {
            bitSets = new IndexedBitSet[size];
            this.size = size;

            for (int index = 0; index < size; index++) {
                bitSets[index] = IndexedBitSet.Square(numRoutes);
            }
        }

        // degreeIndex runs 1, 2, 3,....
        public IndexedBitSet getDegree(int degree) {
            return bitSets[degree-1];
        }

        public void clear() {
            for (int index = 1; index < size; index++) {
                bitSets[index].clear();
            }
        }

        public boolean isSet(final int degree, final RouteIndexPair routePair) {
            return isOverlap(getDegree(degree),routePair);
        }

        public long size() {
            long result = 0;
            for (int index = 0; index < size; index++) {
                result = result + bitSets[index].numberOfBitsSet();
            }
            return result;
        }

        @Override
        public void cacheTo(DataSaver<CostsPerDegreeData> saver) {

            saver.open();

            for (int index = 0; index < size; index++) {
                IndexedBitSet bitSet = bitSets[index];
                for (int routeIndex = 0; routeIndex < numRoutes; routeIndex++) {
                    SimpleImmutableBitmap bitmapForRow = bitSet.getBitSetForRow(routeIndex).getContained();
                    if (bitmapForRow.cardinality()>0) {
                        List<Integer> bitsSetForRow = bitmapForRow.stream().boxed().collect(Collectors.toList());
                        CostsPerDegreeData item = new CostsPerDegreeData(index, routeIndex, bitsSetForRow);
                        saver.write(item);
                    }
                }
            }

            saver.close();
        }

        @Override
        public String getFilename() {
            return "costs_per_degree.csv";
        }

        @Override
        public void loadFrom(Stream<CostsPerDegreeData> stream) {
            stream.forEach(item -> {
                int index = item.getIndex();
                int routeIndex = item.getRouteIndex();
                List<Integer> setBits = item.getSetBits();
                IndexedBitSet bitset = bitSets[index];
                setBits.forEach(bit -> bitset.set(routeIndex, bit));
            });
        }
    }
}
