package com.tramchester.graph.search.routes;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.*;
import com.tramchester.domain.collections.SimpleList;
import com.tramchester.domain.collections.SimpleListSingleton;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.places.InterchangeStation;
import com.tramchester.domain.places.Station;
import com.tramchester.repository.InterchangeRepository;
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

import static java.lang.String.format;

@LazySingleton
public class RouteCostMatrix {
    private static final Logger logger = LoggerFactory.getLogger(RouteCostMatrix.class);

    public static final byte MAX_VALUE = Byte.MAX_VALUE;

    public static final int MAX_DEPTH = 4;

    // map from route->route pair to interchanges that could make that change
    private final RouteIndexToInterchangeRepository routePairToInterchange;
    private final StationAvailabilityRepository availabilityRepository;
    private final RouteRepository routeRepository;
    private final InterchangeRepository interchangeRepository;

    private final CostsPerDegree costsForDegree;
    private final RouteIndex index;
    private final int maxDepth;
    private final int numRoutes;

    @Inject
    RouteCostMatrix(RouteIndexToInterchangeRepository routePairToInterchange, StationAvailabilityRepository availabilityRepository,
                    RouteRepository routeRepository, InterchangeRepository interchangeRepository, RouteIndex index) {
        this.routePairToInterchange = routePairToInterchange;
        this.availabilityRepository = availabilityRepository;
        this.routeRepository = routeRepository;
        this.interchangeRepository = interchangeRepository;

        this.index = index;
        this.maxDepth = MAX_DEPTH;
        this.numRoutes = routeRepository.numberOfRoutes();

        costsForDegree = new CostsPerDegree(maxDepth);
    }

    @PostConstruct
    private void start() {
        RouteDateAndDayOverlap routeDateAndDayOverlap = new RouteDateAndDayOverlap(index, numRoutes);
        routeDateAndDayOverlap.populateFor();

        addInitialConnectionsFromInterchanges(routeDateAndDayOverlap);
        populateCosts(routeDateAndDayOverlap);
    }

    @PreDestroy
    private void clear() {
        costsForDegree.clear();
    }

    private void addInitialConnectionsFromInterchanges(RouteDateAndDayOverlap routeDateAndDayOverlap) {
        final Set<InterchangeStation> interchanges = interchangeRepository.getAllInterchanges();
        logger.info("Pre-populate route to route costs from " + interchanges.size() + " interchanges");
        final IndexedBitSet forDegreeOne = costsForDegree.getDegree(1);
        interchanges.forEach(interchange -> addOverlapsForInterchange(forDegreeOne, interchange, routeDateAndDayOverlap));
        logger.info("Add " + size() + " connections for interchanges");
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
                    //addInterchangeBetween(dropOffIndex, pickupIndex, interchange);
                }
            }
            // apply dates and days
            forDegreeOne.applyAndTo(dropOffIndex, routeDateAndDayOverlap.overlapsFor(dropOffIndex));
        }
    }



    // create a bitmask for route->route changes that are possible on a given date
    public IndexedBitSet createOverlapMatrixFor(LocalDate date) {
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

    public int getDepth(RouteIndexPair routePair, RouteToRouteCosts.InterchangeOperating interchangeOperating, IndexedBitSet dateOverlaps) {

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

        if (possibleChanges.size() != filteredByAvailability.size()) {
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
        for (int depth = 1; depth <= maxDepth; depth++) {
            if (costsForDegree.isSet(depth, routePair)) {
                return (byte) depth;
            }
        }
        return MAX_VALUE;
    }

    /***
     * @param routePair indexes for first and second routes
     * @param dateOverlaps bit mask indicating that routeA->routeB is available at date
     * @return each set returned contains specific interchanges between 2 specific routes
     */
    Collection<SimpleList<RouteIndexPair>> getChangesFor(final RouteIndexPair routePair, IndexedBitSet dateOverlaps) {

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
        final IndexedBitSet overlapsAtDegree = costsForDegree.getDegree(nextDegree); //.and(dateOverlaps);

        final List<SimpleList<RouteIndexPair>> resultsForPair = new ArrayList<>();

        // for >1 result is the set of paris where each of the supplied pairs overlaps
        final List<Integer> overlappingIndexes = getIndexOverlapsFor(overlapsAtDegree, original);
        overlappingIndexes.forEach(overlapForPair -> {

            Pair<RouteIndexPair, RouteIndexPair> toExpand = formNewRoutePairs(original, overlapForPair);

            if (dateOverlaps.isSet(toExpand.getLeft()) && dateOverlaps.isSet(toExpand.getRight())) {
                final Collection<SimpleList<RouteIndexPair>> leftExpansions = expandOnePair(toExpand.getLeft(), nextDegree, dateOverlaps);
                final Collection<SimpleList<RouteIndexPair>> rightExpansions = expandOnePair(toExpand.getRight(), nextDegree, dateOverlaps);

                final Collection<SimpleList<RouteIndexPair>> resultsForOneOverlap = new ArrayList<>(leftExpansions.size() * rightExpansions.size());

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
        RouteIndexPair newFirstPair = RouteIndexPair.of(pair.first(), overlapForPair);
        RouteIndexPair newSecondPair = RouteIndexPair.of(overlapForPair, pair.second());
        return Pair.of(newFirstPair, newSecondPair);
    }

    RouteAndInterchanges getInterchangeFor(RouteIndexPair indexPair) {

        if (routePairToInterchange.hasInterchangesFor(indexPair)) {
            final RoutePair routePair = index.getPairFor(indexPair);

            final Set<Station> changes = routePairToInterchange.getInterchanges(indexPair);
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
        ImmutableBitSet linksForA = costsForDegree.getBitSetForRow(pair.first());
        ImmutableBitSet linksForB = costsForDegree.getBitSetForRow(pair.second());
        ImmutableBitSet overlap = linksForA.and(linksForB);
        return overlap.stream().boxed().collect(Collectors.toList());
    }

    private void populateCosts(RouteDateAndDayOverlap routeDateAndDayOverlap) {
        final int size = routeRepository.numberOfRoutes();
        logger.info("Find costs between " + size + " routes");
        final int fullyConnected = size * size;


        for (byte currentDegree = 1; currentDegree < maxDepth; currentDegree++) {
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

        final IndexedBitSet currentMatrix = costsForDegree.getDegree(currentDegree);
        final IndexedBitSet newMatrix = costsForDegree.getDegree(nextDegree);

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

    private class CostsPerDegree {
        private final IndexedBitSet[] bitSets;
        private final int size;

        private CostsPerDegree(int size) {
            bitSets = new IndexedBitSet[size];
            this.size = size;

            for (int index = 0; index < size; index++) {
                bitSets[index] = new IndexedBitSet(numRoutes);
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
            return getDegree(degree).isSet(routePair);
        }

        public int size() {
            int result = 0;
            for (int index = 0; index < size; index++) {
                result = result + bitSets[index].numberOfConnections();
            }
            return result;
        }
    }
}
