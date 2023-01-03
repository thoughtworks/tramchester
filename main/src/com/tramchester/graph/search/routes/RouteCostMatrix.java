package com.tramchester.graph.search.routes;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.caching.DataCache;
import com.tramchester.dataexport.DataSaver;
import com.tramchester.dataimport.data.CostsPerDegreeData;
import com.tramchester.domain.Route;
import com.tramchester.domain.RoutePair;
import com.tramchester.domain.collections.ImmutableBitSet;
import com.tramchester.domain.collections.IndexedBitSet;
import com.tramchester.domain.collections.RouteIndexPair;
import com.tramchester.domain.collections.tree.PairTree;
import com.tramchester.domain.collections.tree.PairTreeFactory;
import com.tramchester.domain.collections.tree.PairTreeLeaf;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.InterchangeStation;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.filters.GraphFilterActive;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.RouteRepository;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.String.format;

@LazySingleton
public class RouteCostMatrix {
    private static final Logger logger = LoggerFactory.getLogger(RouteCostMatrix.class);

    public static final byte MAX_VALUE = Byte.MAX_VALUE;

    public static final int MAX_DEPTH = 5;

    private final RouteRepository routeRepository;
    private final InterchangeRepository interchangeRepository;
    private final DataCache dataCache;
    private final GraphFilterActive graphFilter;

    private final CostsPerDegree costsForDegree;
    private final RouteIndex index;
    private final int maxDepth;
    private final int numRoutes;
    //private final List<Cache<RouteIndexPair, Set<PairTree>>> cacheForDegree;

    @Inject
    RouteCostMatrix(RouteRepository routeRepository, InterchangeRepository interchangeRepository, DataCache dataCache,
                    GraphFilterActive graphFilter, RouteIndex index) {
        this.routeRepository = routeRepository;
        this.interchangeRepository = interchangeRepository;
        this.dataCache = dataCache;
        this.graphFilter = graphFilter;

        this.index = index;
        this.maxDepth = MAX_DEPTH;
        this.numRoutes = routeRepository.numberOfRoutes();

//        cacheForDegree = new ArrayList<>(MAX_DEPTH);
//        for (int i = 0; i < MAX_DEPTH; i++) {
//            Cache<RouteIndexPair, Set<PairTree>> cache = Caffeine.newBuilder().maximumSize(numRoutes).
//                    expireAfterWrite(10, TimeUnit.MINUTES).recordStats().build();
//            cacheForDegree.add(cache);
//        }

        costsForDegree = new CostsPerDegree(maxDepth);
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
    public IndexedBitSet createOverlapMatrixFor(TramDate date, Set<TransportMode> requestedModes) {
        final Set<Integer> availableOnDate = new HashSet<>();
        for (int routeIndex = 0; routeIndex < numRoutes; routeIndex++) {
            final Route route = index.getRouteFor(routeIndex);
            if (route.isAvailableOn(date) && requestedModes.contains(route.getTransportMode())) {
                availableOnDate.add(routeIndex);
            }
        }
        IndexedBitSet matrix = IndexedBitSet.Square(numRoutes);
        for (int firstRouteIndex = 0; firstRouteIndex < numRoutes; firstRouteIndex++) {
            BitSet result = new BitSet(numRoutes);
            if (availableOnDate.contains(firstRouteIndex)) {
                for (int secondRouteIndex = 0; secondRouteIndex < numRoutes; secondRouteIndex++) {
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
        return costsForDegree.size();
    }

    private byte getDegree(RouteIndexPair routePair) {
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
    public List<Integer> getAllDegrees(RouteIndexPair routeIndexPair) {
        if (routeIndexPair.isSame()) {
            throw new RuntimeException("Don't call with same routes");
        }
        List<Integer> results = new ArrayList<>();
        for (int depth = 1; depth <= maxDepth; depth++) {
            if (costsForDegree.isSet(depth, routeIndexPair)) {
                results.add(depth);
            }
        }
        return results;
    }

    /***
     * @param routePair indexes for first and second routes
     * @param dateOverlaps bit mask indicating that routeA->routeB is available at date
     * @return each set returned contains specific interchanges between 2 specific routes
     */
    public Stream<List<RouteIndexPair>> getChangesFor(final RouteIndexPair routePair, IndexedBitSet dateOverlaps) {

        PairTreeFactory factory = new PairTreeFactory();

        final byte initialDepth = getDegree(routePair); // first 'depth' or number of changes where we find the requested pair

        if (initialDepth == 0) {
            //logger.warn(format("getChangesFor: No changes needed indexes %s", routePair));
            return Stream.empty();
        }
        if (initialDepth == Byte.MAX_VALUE) {
            if (!graphFilter.isActive()) {
                logger.warn(format("getChangesFor: no changes possible indexes %s", index.getPairFor(routePair)));
            }
            return Stream.empty();
        }

        if (logger.isDebugEnabled()) {
            logger.debug(format("Expand for %s initial depth %s", routePair, initialDepth));
        }

        Stream<PairTree> expanded = expandTree(factory.createLeaf(routePair), initialDepth, dateOverlaps);

        Stream<List<RouteIndexPair>> possibleInterchangePairs = expanded.map(PairTree::flatten);

        if (logger.isDebugEnabled()) {
            logger.debug(format("Got set of changes for %s: %s",  routePair, possibleInterchangePairs));
        }

        return possibleInterchangePairs;
    }

    private Stream<PairTree> expandTree(final PairTree tree, final int degree, final IndexedBitSet dateOverlaps) {
        if (degree == 1) {
            // at degree one we are at direct connections between routes via an interchange so the result is those pairs
            return Stream.of(tree);
        }
        if (degree <= 0) {
            throw new RuntimeException("Invalid degree " + degree + " for " + tree);
        }

        return tree.visit(treeToVisit -> expandLeaf(treeToVisit, degree, dateOverlaps)).stream();
    }

    private Set<PairTree> expandLeaf(final PairTreeLeaf treeToVisit, final int degree, final IndexedBitSet dateOverlaps) {
        // find matrix at one higher than where the pair meet, extract row/column corresponding i.e. next changes needed
        final RouteIndexPair leafPair = treeToVisit.get();

//        Set<PairTree> cachedResult = cacheForDegree.get(degree).getIfPresent(leafPair);
//        if (cachedResult!=null) {
//            return cachedResult;
//        }

        final IndexedBitSet changesForDegree = costsForDegree.getDegree(degree - 1).getRowAndColumn(leafPair.first(), leafPair.second());
        // apply mask to filter out unavailable dates/modes
        final IndexedBitSet withDateApplied = changesForDegree.and(dateOverlaps);
        // get the possible pairs where next change can happen
        Stream<Pair<Integer, Integer>> pairsForDegree = withDateApplied.getPairs();
        Stream<RouteIndexPair> routePairs = pairsForDegree.map(pair -> RouteIndexPair.of(pair.getLeft(), pair.getRight()));
        // group pairs where second/first match i.e. 8,5 5,4
        final List<RouteIndexPair.Group> grouped = RouteIndexPair.createAllUniqueGroups(routePairs);

        // set of unique trees resulting from this expansion
        Stream<PairTree> expanded = grouped.stream().
                map(group -> treeToVisit.replace(leafPair, group.first(), group.second())).
                map(PairTree.Mutated::get);

        // in turn expand each resulting tree
        final Set<PairTree> fullyExpanded = expanded.
                flatMap(expandedTree -> expandTree(expandedTree, degree - 1, dateOverlaps)).
                collect(Collectors.toSet());

        //cacheForDegree.get(degree).put(leafPair, fullyExpanded);

        return fullyExpanded;
    }

    private boolean isOverlap(IndexedBitSet bitSet, RouteIndexPair pair) {
        return bitSet.isSet(pair.first(), pair.second());
    }

    private void populateCosts(RouteDateAndDayOverlap routeDateAndDayOverlap) {
        final int size = routeRepository.numberOfRoutes();
        logger.info("Find costs between " + size + " routes");
        final int fullyConnected = size * size;

        int previousTotal = 0;
        for (byte currentDegree = 1; currentDegree < maxDepth; currentDegree++) {
            addConnectionsFor(routeDateAndDayOverlap, currentDegree);
            final int currentTotal = size();
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

        final int finalSize = size();
        logger.info("Added cost for " + finalSize + " route combinations");
        if (finalSize < fullyConnected) {
            double percentage = ((double) finalSize / (double) fullyConnected);
            logger.warn(format("Not fully connected, only %s (%s) of %s ", finalSize, percentage, fullyConnected));
        } else {
            logger.info(format("Fully connected, with %s of %s ", finalSize, fullyConnected));
        }
    }

    private void addConnectionsFor(RouteDateAndDayOverlap routeDateAndDayOverlap, byte currentDegree) {
        final Instant startTime = Instant.now();
        final int nextDegree = currentDegree + 1;

        final IndexedBitSet currentMatrix = costsForDegree.getDegree(currentDegree);
        final IndexedBitSet newMatrix = costsForDegree.getDegree(nextDegree);

        for (int routeIndex = 0; routeIndex < numRoutes; routeIndex++) {

            final ImmutableBitSet currentConnectionsForRoute = currentMatrix.getBitSetForRow(routeIndex);
            final BitSet resultForForRoute = new BitSet(numRoutes);
            for (int connectionIndex = 0; connectionIndex < numRoutes; connectionIndex++) {
                if (currentConnectionsForRoute.isSet(connectionIndex)) {
                    // if current routeIndex is connected to a route, then for next degree include that other routes connections
                    final ImmutableBitSet otherRoutesConnections = currentMatrix.getBitSetForRow(connectionIndex);
                    otherRoutesConnections.applyOrTo(resultForForRoute);
                }
            }
            final BitSet dateOverlapMask = routeDateAndDayOverlap.overlapsFor(routeIndex);  // only those routes whose dates overlap
            resultForForRoute.and(dateOverlapMask);

            final ImmutableBitSet allExistingConnectionsForRoute = getExistingBitSetsForRoute(routeIndex, currentDegree);

            allExistingConnectionsForRoute.applyAndNotTo(resultForForRoute);  // don't include any current connections for this route

            newMatrix.insert(routeIndex, resultForForRoute);
        }

        final long took = Duration.between(startTime, Instant.now()).toMillis();
        logger.info("Added connections " + newMatrix.numberOfBitsSet() + "  Degree " + nextDegree + " in " + took + " ms");
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

    private class CostsPerDegree implements DataCache.Cacheable<CostsPerDegreeData> {
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

        public boolean isSet(int degree, RouteIndexPair routePair) {
            return isOverlap(getDegree(degree),routePair);
        }

        public int size() {
            int result = 0;
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
                    BitSet bitSetForRow = bitSet.getBitSetForRow(routeIndex).getContained();
                    if (bitSetForRow.cardinality()>0) {
                        List<Integer> bitsSetForRow = bitSetForRow.stream().boxed().collect(Collectors.toList());
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
