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
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.String.format;

@LazySingleton
public class RouteCostMatrix  {
    private static final Logger logger = LoggerFactory.getLogger(RouteCostMatrix.class);

    public static final byte MAX_VALUE = Byte.MAX_VALUE;

    public static final int MAX_DEPTH = 5;

    private final InterchangeRepository interchangeRepository;
    private final DataCache dataCache;
    private final GraphFilterActive graphFilter;

    private final CostsPerDegree costsForDegree;
    private final RouteIndex routeIndex;
    private final int numRoutes;

    private final ConnectingLinks connectingLinks;

    @Inject
    RouteCostMatrix(NumberOfRoutes numberOfRoutes, InterchangeRepository interchangeRepository, DataCache dataCache,
                    GraphFilterActive graphFilter, RouteIndexPairFactory pairFactory, RouteIndex routeIndex) {
        this.interchangeRepository = interchangeRepository;
        this.dataCache = dataCache;
        this.graphFilter = graphFilter;
        this.routeIndex = routeIndex;
        this.numRoutes = numberOfRoutes.numberOfRoutes();

        costsForDegree = new CostsPerDegree();
        connectingLinks = new ConnectingLinks(pairFactory,numRoutes, routeIndex, interchangeRepository, this);

    }

    @PostConstruct
    private void start() {

        RouteDateAndDayOverlap routeDateAndDayOverlap = new RouteDateAndDayOverlap(routeIndex, numRoutes);
        routeDateAndDayOverlap.populateFor();

        connectingLinks.start();

        if (graphFilter.isActive()) {
            logger.warn("Filtering is enabled, skipping all caching");
            createCostMatrix(routeDateAndDayOverlap);
        } else {
            if (dataCache.has(costsForDegree)) {
                logger.info("Loading from cache");
                dataCache.loadInto(costsForDegree, CostsPerDegreeData.class);
            } else {
                logger.info("Not in cache, creating");
                createCostMatrix(routeDateAndDayOverlap);
                dataCache.save(costsForDegree, CostsPerDegreeData.class);
            }
        }

        createBacktracking(routeDateAndDayOverlap);

    }

    private void createCostMatrix(RouteDateAndDayOverlap routeDateAndDayOverlap) {
        IndexedBitSet forDegreeOne = costsForDegree.getDegreeMutable(1);
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


    private void createBacktracking(RouteDateAndDayOverlap routeDateAndDayOverlap) {
        // degree 1 = depth 0 = interchanges directly
        for (int currentDegree = 1; currentDegree <= MAX_DEPTH; currentDegree++) {
            createBacktracking(routeDateAndDayOverlap, currentDegree);
        }
    }

    private void createBacktracking(RouteDateAndDayOverlap routeDateAndDayOverlap, final int currentDegree) {
        final int totalSize = numRoutes * numRoutes;
        if (currentDegree<1) {
            throw new RuntimeException("Only call for >1 , got " + currentDegree);
        }
        logger.info("Create backtrack pair map for degree " + currentDegree);

        //final int nextDegree = currentDegree + 1;
        final ImmutableIndexedBitSet matrixForDegree = costsForDegree.getDegree(currentDegree);

        if (matrixForDegree.numberOfBitsSet()>0) {
            // zero indexed
            final RouteConnectingLinks routeConnectingLinks = connectingLinks.forDegree(currentDegree);

            final Instant startTime = Instant.now();

            for (short currentRoute = 0; currentRoute < numRoutes; currentRoute++) {
                final short currentRouteIndex = currentRoute;

                final SimpleImmutableBitmap dateOverlapsForRoute = routeDateAndDayOverlap.overlapsFor(currentRouteIndex);

                final SimpleImmutableBitmap currentConnections = matrixForDegree.getBitSetForRow(currentRouteIndex);

                currentConnections.getBitIndexes().
                        filter(dateOverlapsForRoute::get).
                        forEach(connectedRoute -> {
                            final SimpleImmutableBitmap dateOverlapsForConnectedRoute = routeDateAndDayOverlap.overlapsFor(connectedRoute);
                            final SimpleImmutableBitmap intermediates = matrixForDegree.getBitSetForRow(connectedRoute);
                            routeConnectingLinks.addLinksBetween(currentRouteIndex, connectedRoute, intermediates,
                                    dateOverlapsForRoute, dateOverlapsForConnectedRoute);
                });
            }

            final long took = Duration.between(startTime, Instant.now()).toMillis();
            int added = routeConnectingLinks.size();
            double percentage = ((double)added)/((double)totalSize) * 100D;
            logger.info(String.format("Added backtrack paris %s (%s %%) Degree %s in %s ms",
                    added, percentage, currentDegree, took));

        } else {
            logger.info("No bits set for degree " + currentDegree);
        }
    }




    private void addOverlapsForRoutes(IndexedBitSet forDegreeOne, RouteDateAndDayOverlap routeDateAndDayOverlap,
                                      Set<Route> dropOffAtInterchange, Set<Route> pickupAtInterchange) {
        for (final Route dropOff : dropOffAtInterchange) {
            final int dropOffIndex = routeIndex.indexFor(dropOff.getId());
            // todo, could use bitset Or and And with DateOverlapMask here
            for (final Route pickup : pickupAtInterchange) {
                if ((!dropOff.equals(pickup)) && pickup.isDateOverlap(dropOff)) {
                    final int pickupIndex = routeIndex.indexFor(pickup.getId());
                    forDegreeOne.set(dropOffIndex, pickupIndex);
                }
            }
            // apply dates and days
            forDegreeOne.applyAndToRow(dropOffIndex, routeDateAndDayOverlap.overlapsFor(dropOffIndex));
        }
    }

    // create a bitmask for route->route changes that are possible on a given date and transport mode
    public IndexedBitSet createOverlapMatrixFor(TramDate date, Set<TransportMode> requestedModes) {

        final Set<Short> availableOnDate = new HashSet<>();
        for (short routeIndex = 0; routeIndex < numRoutes; routeIndex++) {
            final Route route = this.routeIndex.getRouteFor(routeIndex);
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

    public long size() {
        return costsForDegree.size();
    }

    private byte getDegree(final RouteIndexPair routePair) {
        if (routePair.isSame()) {
            return 0;
        }
        for (int degree = 1; degree <= MAX_DEPTH; degree++) {
            if (costsForDegree.isSet(degree, routePair)) {
                return (byte) degree;
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
        for (int degree = 1; degree <= MAX_DEPTH; degree++) {
            if (costsForDegree.isSet(degree, routeIndexPair)) {
                results.add(degree);
            }
        }
        return results;
    }

    private boolean isOverlap(final ImmutableIndexedBitSet bitSet, final RouteIndexPair pair) {
        return bitSet.isSet(pair);
    }

    private void populateCosts(RouteDateAndDayOverlap routeDateAndDayOverlap) {
        final int size = numRoutes;
        final int fullyConnected = size * size;

        logger.info("Find costs between " + size + " routes (" + fullyConnected + ")");

        long previousTotal = 0;
        for (byte currentDegree = 1; currentDegree <= MAX_DEPTH; currentDegree++) {
            addConnectionsFor(routeDateAndDayOverlap, currentDegree);
            final long currentTotal = size();
            logger.info("Total number of connections " + currentTotal);
            if (currentTotal >= fullyConnected) {
                break;
            }
            if (previousTotal==currentTotal) {
                logger.warn(format("No improvement in connections at degree %s and number %s", currentDegree, currentTotal));
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

        final ImmutableIndexedBitSet currentMatrix = costsForDegree.getDegree(currentDegree);
        final IndexedBitSet newMatrix = costsForDegree.getDegreeMutable(nextDegree);

        for (int route = 0; route < numRoutes; route++) {
            final SimpleBitmap resultForForRoute = SimpleBitmap.create(numRoutes);
            final SimpleImmutableBitmap currentConnectionsForRoute = currentMatrix.getBitSetForRow(route);

            currentConnectionsForRoute.getBitIndexes().forEach(connectedRoute -> {
                // if current route is connected to another route, then for next degree include that other route's connections
                final SimpleImmutableBitmap otherRoutesConnections = currentMatrix.getBitSetForRow(connectedRoute);
                //otherRoutesConnections.applyOrTo(resultForForRoute);
                resultForForRoute.or(otherRoutesConnections);
            });

            final SimpleImmutableBitmap dateOverlapMask = routeDateAndDayOverlap.overlapsFor(route);  // only those routes whose dates overlap
            resultForForRoute.and(dateOverlapMask);

            final SimpleImmutableBitmap allExistingConnectionsForRoute = getExistingBitSetsForRoute(route, currentDegree);

            resultForForRoute.andNot(allExistingConnectionsForRoute);
            //allExistingConnectionsForRoute.applyAndNotTo(resultForForRoute);  // don't include any current connections for this route

            newMatrix.insert(route, resultForForRoute);
        }

        final long took = Duration.between(startTime, Instant.now()).toMillis();
        logger.info("Added " + newMatrix.numberOfBitsSet() + " connections for  degree " + nextDegree + " in " + took + " ms");
    }

    public SimpleImmutableBitmap getExistingBitSetsForRoute(final int routeIndex, final int startingDegree) {
        SimpleBitmap result = SimpleBitmap.create(numRoutes);

        for (int degree = startingDegree; degree > 0; degree--) {
            ImmutableIndexedBitSet allConnectionsAtDegree = costsForDegree.getDegree(degree);
            SimpleImmutableBitmap existingConnectionsAtDepth = allConnectionsAtDegree.getBitSetForRow(routeIndex);
            result.or(existingConnectionsAtDepth);
        }

        return result;
    }

    public int getConnectionDepthFor(Route routeA, Route routeB) {
        RouteIndexPair routePair = routeIndex.getPairFor(RoutePair.of(routeA, routeB));
        return getDegree(routePair);
    }

    public PathResults getInterchangesFor(final RouteIndexPair indexPair, final ImmutableIndexedBitSet dateOverlaps,
                                          final Function<InterchangeStation, Boolean> interchangeFilter) {
        final int degree = getDegree(indexPair);

        if (degree==Byte.MAX_VALUE) {
            logger.warn("No degree found for " + routeIndex.getPairFor(indexPair));
            return new PathResults.NoPathResults();
        }

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Get interchanges for %s with initial degree %s", HasId.asIds(routeIndex.getPairFor(indexPair)), degree));
        }

        final ImmutableIndexedBitSet changesForDegree = costsForDegree.getDegree(degree).getCopyOfRowAndColumn(indexPair.first(), indexPair.second());
        // apply mask to filter out unavailable dates/modes quickly
        final IndexedBitSet withDateApplied = IndexedBitSet.and(changesForDegree, dateOverlaps);

        if (withDateApplied.isSet(indexPair)) {

            QueryPathsWithDepth.QueryPath pathFor = getPathFor(indexPair, degree, interchangeFilter);
            if (pathFor.hasAny()) {
                return new PathResults.HasPathResults(pathFor);
            } else {
                return new PathResults.NoPathResults();
            }

        } else {
            return new PathResults.NoPathResults();
        }
    }

    private QueryPathsWithDepth.QueryPath getPathFor(final RouteIndexPair indexPair, final int currentDegree,
                                                     final Function<InterchangeStation, Boolean> interchangeFilter) {

        if (currentDegree==1) {
            if (!interchangeRepository.hasInterchangeFor(indexPair)) {
                final RoutePair routePair = routeIndex.getPairFor(indexPair);
                final String msg = "Unable to find interchange for " + HasId.asIds(routePair);
                logger.error(msg);
                logger.warn("Full pair first:" + routePair.first() + " second: " + routePair.second());
                if (!routePair.isDateOverlap()) {
                    logger.error("Further: No date overlap between " + HasId.asIds(routePair));
                }
                throw new RuntimeException(msg);
            }

            // can be multiple interchanges points between a pair of routes
            final Set<InterchangeStation> changes = interchangeRepository.getInterchangesFor(indexPair).
                    filter(interchangeFilter::apply).
                    collect(Collectors.toSet());

            if (changes.isEmpty()) {
                return QueryPathsWithDepth.ZeroPaths.get();
            } else {
                return QueryPathsWithDepth.AnyOfInterchanges.Of(changes);
            }
        } else {
            final int lowerDegree = currentDegree-1;
            //final int depth = degree - 1;

            final Stream<Pair<RouteIndexPair, RouteIndexPair>> underlying = connectingLinks.forDegree(lowerDegree, indexPair); //.forDepth(depth-1, indexPair);
            // TODO parallel? not required?
            Set<QueryPathsWithDepth.BothOf> combined = underlying. //.parallel().
                    map(pair -> expandPathFor(pair, lowerDegree, interchangeFilter)).
                    filter(QueryPathsWithDepth.BothOf::hasAny).
                    collect(Collectors.toSet());

            return new QueryPathsWithDepth.AnyOf(combined);
        }
    }

    @NotNull
    private QueryPathsWithDepth.BothOf expandPathFor(final Pair<RouteIndexPair, RouteIndexPair> pair, final int degree,
                                                     final Function<InterchangeStation, Boolean> interchangeFilter) {
        final QueryPathsWithDepth.QueryPath pathA = getPathFor(pair.getLeft(), degree, interchangeFilter);
        final QueryPathsWithDepth.QueryPath pathB = getPathFor(pair.getRight(), degree, interchangeFilter);
        return new QueryPathsWithDepth.BothOf(pathA, pathB);
    }

    // test support
    public Set<Pair<RoutePair, RoutePair>> getBackTracksFor(int degree, RouteIndexPair indexPair) {
        return connectingLinks.forDegree(degree, indexPair).
                map(pair -> Pair.of(routeIndex.getPairFor(pair.getLeft()), routeIndex.getPairFor(pair.getRight()))).
                collect(Collectors.toSet());
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

        public SimpleImmutableBitmap overlapsFor(int routeIndex) {
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

        private final boolean[][] seen; // performance

        private RouteConnectingLinks(RouteIndexPairFactory pairFactory, int numRoutes) {
            this.pairFactory = pairFactory;
            this.numRoutes = numRoutes;
            bitSetForIndex = new HashMap<>();
            seen = new boolean[numRoutes][numRoutes];
        }

        public void addLinksBetween(final short routeIndexA, final short routeIndexB, final SimpleImmutableBitmap links,
                                    final SimpleImmutableBitmap dateOverlapsForRoute, final SimpleImmutableBitmap dateOverlapsForConnectedRoute) {
            links.getBitIndexes().
                    filter(linkIndex -> dateOverlapsForRoute.get(linkIndex) && dateOverlapsForConnectedRoute.get(linkIndex)).
                    map(linkIndex -> getBitSetForPair(routeIndexA, linkIndex)).
                    forEach(bitSet -> bitSet.set(routeIndexB));
        }

        private BitSet getBitSetForPair(short routeIndexA, short linkIndex) {
            int position = getPosition(routeIndexA, linkIndex);
            if (seen[routeIndexA][linkIndex]) {
                return bitSetForIndex.get(position);
            }

            final BitSet bitSet = new BitSet();
            bitSetForIndex.put(position, bitSet);
            seen[routeIndexA][linkIndex] = true;
            return bitSet;

        }

        private int getPositionFor(RouteIndexPair routeIndexPair) {
            return getPosition(routeIndexPair.first(), routeIndexPair.second());
        }

        private int getPosition(short indexA, short indexB) {
            return (indexA * numRoutes) + indexB;
        }

        // re-expand from (A,C) -> B into: (A,B) (B,C)
        public Stream<Pair<RouteIndexPair, RouteIndexPair>> getLinksFor(RouteIndexPair indexPair) {
            final int position = getPositionFor(indexPair);

            final BitSet connectingRoutes = bitSetForIndex.get(position);
            return connectingRoutes.stream().
                    mapToObj(link -> (short) link).
                    map(link -> Pair.of(pairFactory.get(indexPair.first(), link), pairFactory.get(link, indexPair.second())));
        }

        public int size() {
            return bitSetForIndex.size();
        }

        public boolean hasLinksFor(RouteIndexPair indexPair) {
            return seen[indexPair.first()][indexPair.second()];
        }
    }

    private static class ConnectingLinks {
        private final List<RouteConnectingLinks> links;
        private final RouteIndex routeIndex;
        private final RouteIndexPairFactory pairFactory;
        private final int numRoutes;
        private final InterchangeRepository pairToInterchange;
        private final RouteCostMatrix parent;

        public ConnectingLinks(RouteIndexPairFactory pairFactory, int numRoutes, RouteIndex routeIndex, InterchangeRepository pairToInterchange,
                               RouteCostMatrix parent) {
            this.pairFactory = pairFactory;
            this.numRoutes = numRoutes;
            this.pairToInterchange = pairToInterchange;
            this.parent = parent;
            links = new ArrayList<>(MAX_DEPTH);
            this.routeIndex = routeIndex;
        }

        public void start() {
            for (int depth = 0; depth < MAX_DEPTH; depth++) {
                links.add(new RouteConnectingLinks(pairFactory, numRoutes));
            }
        }

        private RouteConnectingLinks forDepth(int depth) {
            guardDepth(depth);
            return links.get(depth);
        }

        private void guardDepth(int depth) {
            if (depth<0 || depth>MAX_DEPTH) {
                String message = "Depth:" + depth + " is out of range";
                logger.error(message);
                throw new RuntimeException(message);
            }
        }

        private Stream<Pair<RouteIndexPair, RouteIndexPair>> forDepth(int depth, RouteIndexPair indexPair) {
            guardDepth(depth);
            if (links.get(depth).hasLinksFor(indexPair)) {
                return links.get(depth).getLinksFor(indexPair);
            }

            int degree = parent.getDegree(indexPair);
            final RoutePair missing = routeIndex.getPairFor(indexPair);
            final String message = MessageFormat.format("Missing indexPair {0} ({1}) at depth {2} cost degree {3}",
                    indexPair, missing, depth, degree);
            logger.error(message);
            if (!missing.isDateOverlap()) {
                logger.warn("Also no date overlap between " + missing);
            }
            if (!pairToInterchange.hasInterchangeFor(indexPair)) {
                logger.warn("Also no interchange for " +  missing);
            }
            throw new RuntimeException(message);
        }

        public RouteConnectingLinks forDegree(int currentDegree) {
            return forDepth(currentDegree-1);
        }

        public Stream<Pair<RouteIndexPair, RouteIndexPair>> forDegree(int degree, RouteIndexPair indexPair) {
            return forDepth(degree-1, indexPair);
        }
    }

    /***
     * encapsulate cost per degree to facilitate caching
     */
    private class CostsPerDegree implements DataCache.CachesData<CostsPerDegreeData> {
        private final IndexedBitSet[] bitSets;

        private CostsPerDegree() {
            bitSets = new IndexedBitSet[MAX_DEPTH];

            for (int depth = 0; depth < MAX_DEPTH; depth++) {
                bitSets[depth] = IndexedBitSet.Square(numRoutes);
            }
        }

        // degreeIndex runs 1, 2, 3,....
        public ImmutableIndexedBitSet getDegree(int degree) {
            return bitSets[degree-1];
        }

        public IndexedBitSet getDegreeMutable(int degree) {
            return bitSets[degree-1];
        }

        public void clear() {
            for (int depth = 0; depth < MAX_DEPTH; depth++) {
                bitSets[depth].clear();
            }
        }

        public boolean isSet(final int degree, final RouteIndexPair routePair) {
            return isOverlap(getDegree(degree), routePair);
        }

        public long size() {
            long result = 0;
            for (int depth = 0; depth < MAX_DEPTH; depth++) {
                result = result + bitSets[depth].numberOfBitsSet();
            }
            return result;
        }

        @Override
        public void cacheTo(DataSaver<CostsPerDegreeData> saver) {

            saver.open();

            for (int depth = 0; depth < MAX_DEPTH; depth++) {
                IndexedBitSet bitSet = bitSets[depth];
                for (int routeIndex = 0; routeIndex < numRoutes; routeIndex++) {
                    SimpleImmutableBitmap bitmapForRow = bitSet.getBitSetForRow(routeIndex); //.getContained();
                    if (bitmapForRow.cardinality()>0) {
                        List<Short> bitsSetForRow = bitmapForRow.getBitIndexes().collect(Collectors.toList());
                        CostsPerDegreeData item = new CostsPerDegreeData(depth, routeIndex, bitsSetForRow);
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
                List<Short> setBits = item.getSetBits();
                IndexedBitSet bitset = bitSets[index];
                setBits.forEach(bit -> bitset.set(routeIndex, bit));
            });
        }


    }


}
