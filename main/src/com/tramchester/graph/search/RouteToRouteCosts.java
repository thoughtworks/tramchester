package com.tramchester.graph.search;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.caching.DataCache;
import com.tramchester.dataexport.DataSaver;
import com.tramchester.dataimport.data.RouteIndexData;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.NumberOfChanges;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.InterchangeStation;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.StationGroup;
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
    private final Index index;
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
        index = new Index(routeRepository);
        costs = new Costs(numberOfRoutes, MAX_DEPTH+1);
    }

   @PostConstruct
    public void start() {
        logger.info("starting");
        if (graphFilter.isFiltered()) {
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
            if (currentTotal>=fullyConnected) {
                break;
            }
        }

        final int finalSize = costs.size();
        logger.info("Added cost for " + finalSize + " route combinations");
        if (finalSize < fullyConnected) {
            double percentage = ((double)finalSize/(double)fullyConnected);
            logger.warn(format("Not fully connected, only %s (%s) of %s ", finalSize, percentage, fullyConnected));
        }
    }

    private void addConnectionsFor(RouteDateAndDayOverlap routeDateAndDayOverlap, byte currentDegree) {
        final Instant startTime = Instant.now();
        final int nextDegree = currentDegree+1;

        final CostsForDegree currentMatrix = costs.costsFor(currentDegree);
        final CostsForDegree newMatrix = costs.costsFor(nextDegree);

        for (int routeIndex = 0; routeIndex < numberOfRoutes; routeIndex++) {

            BitSet currentConnectionsForRoute = currentMatrix.getConnectionsFor(routeIndex);
            BitSet result = new BitSet(numberOfRoutes);
            for (int connectionIndex = 0; connectionIndex < numberOfRoutes; connectionIndex++) {
                if (currentConnectionsForRoute.get(connectionIndex)) {
                    // if current routeIndex is connected to a route, then for next degree include that other routes connections
                    BitSet otherRoutesConnections = currentMatrix.getConnectionsFor(connectionIndex);
                    result.or(otherRoutesConnections);
                }
            }
            final BitSet dateOverlapMask = routeDateAndDayOverlap.overlapsFor(routeIndex);  // only those routes whose dates overlap
            result.and(dateOverlapMask);
            result.andNot(currentConnectionsForRoute);
            newMatrix.insert(routeIndex, result);
        }

        final long took = Duration.between(startTime, Instant.now()).toMillis();
        logger.info("Added connections " + newMatrix.numberOfConnections() + "  Degree " + nextDegree + " in " + took + " ms");
    }

    private void addInitialConnectionsFromInterchanges(RouteDateAndDayOverlap routeDateAndDayOverlap) {
        final Set<InterchangeStation> interchanges = interchangeRepository.getAllInterchanges();
        logger.info("Pre-populate route to route costs from " + interchanges.size() + " interchanges");
        final CostsForDegree forDegreeOne = costs.costsForDegree[1];
        interchanges.forEach(interchange -> addOverlapsFor(forDegreeOne, interchange, routeDateAndDayOverlap));
        logger.info("Add " + costs.size() + " connections for interchanges");
    }

    private void addOverlapsFor(CostsForDegree forDegreeOne, InterchangeStation interchange, RouteDateAndDayOverlap routeDateAndDayOverlap) {

        // record interchanges, where we can go from being dropped off (routes) to being picked up (routes)
        final Set<Route> dropOffAtInterchange = interchange.getDropoffRoutes();
        final Set<Route> pickupAtInterchange = interchange.getPickupRoutes();

        for (final Route dropOff : dropOffAtInterchange) {
            final int dropOffIndex = index.indexFor(dropOff.getId());
            BitSet forDropOffRoute = forDegreeOne.getConnectionsFor(dropOffIndex);
            // todo, could use bitset Or and And with DateOverlapMask here
            for (final Route pickup : pickupAtInterchange) {
                if (!dropOff.equals(pickup)) {
                    final int pickupIndex = index.indexFor(pickup.getId());
                    forDropOffRoute.set(pickupIndex);
                }
            }
            // apply dates and days
            forDropOffRoute.and(routeDateAndDayOverlap.overlapsFor(dropOffIndex));
        }
    }

    public int getFor(Route routeA, Route routeB) {
        if (routeA.equals(routeB)) {
            return 0;
        }
        if (!routeA.isDateOverlap(routeB)) {
            return Integer.MAX_VALUE;
        }
        final IdFor<Route> idA = routeA.getId();
        final IdFor<Route> idB = routeB.getId();
        final byte result = costs.get(index.indexFor(idA), index.indexFor(idB));
        if (result==Costs.MAX_VALUE) {
            if (routeA.getTransportMode()==routeB.getTransportMode() ) {
                // TODO Why so many hits here?
                // for mixed transport mode having no value is quite normal
                final String msg = "Missing (routeId:" + idA + ", routeId:" + idB + ")";
                logger.debug(msg);
            }
            return Integer.MAX_VALUE;
        }
        return result;
    }

    public int size() {
        return costs.size();
    }

    @Override
    public NumberOfChanges getNumberOfChanges(StationGroup start, StationGroup end) {
        return getNumberOfChanges(LocationSet.of(start.getContained()), LocationSet.of(end.getContained()));
    }

    @Override
    public NumberOfChanges getNumberOfChanges(LocationSet starts, LocationSet destinations) {
        if (starts.stream().allMatch(station -> station.getPickupRoutes().isEmpty())) {
            logger.warn(format("start stations %s have no pick-up routes", HasId.asIds(starts)));
            return new NumberOfChanges(Integer.MAX_VALUE, Integer.MAX_VALUE);
        }
        if (destinations.stream().allMatch(station -> station.getDropoffRoutes().isEmpty())) {
            logger.warn(format("destination stations %s have no drop-off routes",  HasId.asIds(destinations)));
            return new NumberOfChanges(Integer.MAX_VALUE, Integer.MAX_VALUE);
        }
        if (neighboursRepository.areNeighbours(starts, destinations)) {
            return new NumberOfChanges(0, maxHops(pickupRoutesFor(starts), dropoffRoutesFor(destinations)));
        }
        return getNumberOfHops(pickupRoutesFor(starts), dropoffRoutesFor(destinations));
    }

    @Override
    public NumberOfChanges getNumberOfChanges(Location<?> startStation, Location<?> destination) {
        if (neighboursRepository.areNeighbours(startStation, destination)) {
            return new NumberOfChanges(1, 1);
        }
        if (startStation.getPickupRoutes().isEmpty()) {
            logger.warn(format("start station %s has no pick-up routes", startStation));
            return new NumberOfChanges(Integer.MAX_VALUE, Integer.MAX_VALUE);
        }
        if (destination.getDropoffRoutes().isEmpty()) {
            logger.warn(format("destination station %s has no drop-off routes", destination));
            return new NumberOfChanges(Integer.MAX_VALUE, Integer.MAX_VALUE);
        }
        return getNumberOfHops(startStation.getPickupRoutes(), destination.getDropoffRoutes());
    }


    @Override
    public LowestCostsForDestRoutes getLowestCostCalcutatorFor(LocationSet destinations) {
        Set<Route> destinationRoutes = destinations.stream().
                map(Location::getDropoffRoutes).flatMap(Collection::stream).collect(Collectors.toUnmodifiableSet());
        return new LowestCostForDestinations(this, destinationRoutes);
    }

    @NotNull
    private NumberOfChanges getNumberOfHops(Set<Route> startRoutes, Set<Route> destinationRoutes) {
        int minHops = minHops(startRoutes, destinationRoutes);
        if (minHops==Integer.MAX_VALUE) {
            logger.warn(format("No results founds between %s and %s", HasId.asIds(startRoutes), HasId.asIds(destinationRoutes)));
        }
        int maxHops = maxHops(startRoutes, destinationRoutes);
        return new NumberOfChanges(minHops, maxHops);
    }

    private int minHops(Set<Route> startRoutes, Set<Route> endRoutes) {
        final Optional<Integer> query = startRoutes.stream().
                flatMap(startRoute -> endRoutes.stream().map(endRoute -> getFor(startRoute, endRoute))).
                min(Integer::compare);
        if (query.isEmpty()) {
            logger.warn(format("No minHops found for %s to %s", HasId.asIds(startRoutes), HasId.asIds(endRoutes)));
        }
        return query.orElse(Integer.MAX_VALUE);
    }

    private Integer maxHops(Set<Route> startRoutes, Set<Route> endRoutes) {
        final Optional<Integer> query = startRoutes.stream().
                flatMap(startRoute -> endRoutes.stream().map(endRoute -> getFor(startRoute, endRoute))).
                filter(result -> result != Integer.MAX_VALUE).
                max(Integer::compare);
        if (query.isEmpty()) {
            logger.warn(format("No maxHops found for %s to %s", HasId.asIds(startRoutes), HasId.asIds(endRoutes)));
        }
        return query.orElse(Integer.MAX_VALUE);
    }

    private Set<Route> dropoffRoutesFor(LocationSet locations) {
        return locations.stream().flatMap(station -> station.getDropoffRoutes().stream()).collect(Collectors.toSet());
    }

    private Set<Route> pickupRoutesFor(LocationSet locations) {
        return locations.stream().flatMap(station -> station.getPickupRoutes().stream()).collect(Collectors.toSet());
    }

    private static class LowestCostForDestinations implements LowestCostsForDestRoutes {
        private final RouteToRouteCosts routeToRouteCosts;
        private final Set<Integer> destinationIndexs;

        public LowestCostForDestinations(BetweenRoutesCostRepository routeToRouteCosts, Set<Route> destinations) {
            this.routeToRouteCosts = (RouteToRouteCosts) routeToRouteCosts;
            destinationIndexs = destinations.stream().
                    map(destination -> this.routeToRouteCosts.index.indexFor(destination.getId())).
                    collect(Collectors.toUnmodifiableSet());
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
                    map(indexOfDest -> routeToRouteCosts.costs.get(indexOfStart, indexOfDest)).
                    filter(result -> result!=Costs.MAX_VALUE).
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
                    map(dest -> routeToRouteCosts.costs.get(indexOfStart, dest)).
                    min().
                    orElse(Integer.MAX_VALUE);
            return Pair.of(result, start);
        }

    }

    private static class Index implements DataCache.Cacheable<RouteIndexData> {
        private final RouteRepository routeRepository;
        private final Map<IdFor<Route>, Integer> mapRouteIdToIndex;
        private final Map<Integer, IdFor<Route>>  mapIndexToRouteId;
        private final int numberOfRoutes;

        private Index(RouteRepository routeRepository) {
            this.routeRepository = routeRepository;
            this.numberOfRoutes = routeRepository.numberOfRoutes();
            mapRouteIdToIndex = new HashMap<>(numberOfRoutes);
            mapIndexToRouteId = new HashMap<>(numberOfRoutes);
        }

        public void populateFrom(RouteRepository routeRepository) {
            logger.info("Creating index");
            List<IdFor<Route>> routesList = routeRepository.getRoutes().stream().map(Route::getId).collect(Collectors.toList());
            createIndex(routesList);
            logger.info("Added " + mapRouteIdToIndex.size() +" index entries");
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
            if (mapRouteIdToIndex.size()!=numberOfRoutes) {
                throw new DataCache.CacheLoadException("Mismatch on number of routes, got " + mapRouteIdToIndex.size() +
                        " expected " + numberOfRoutes);
            }
        }

//        public IdFor<Route> getIdFor(int routeIndex) {
//            return mapIndexToRouteId.get(routeIndex);
//        }

        public void clear() {
            mapRouteIdToIndex.clear();
            mapIndexToRouteId.clear();
        }

        public Route getRouteFor(int index) {
            return routeRepository.getRouteById(mapIndexToRouteId.get(index));
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

        public BitSet getConnectionsFor(int routesIndex) {
            return rows[routesIndex];
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
    }

    private static class Costs {
        public static final byte MAX_VALUE = Byte.MAX_VALUE;

        private final CostsForDegree[] costsForDegree;
        private final int maxDepth;

        private Costs(int numRoutes, int maxDepth) {
            this.maxDepth = maxDepth;
            costsForDegree = new CostsForDegree[maxDepth];
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

        public byte get(int indexA, int indexB) {
            if (indexA==indexB) {
                return 0;
            }
            for (int i = 1; i < maxDepth; i++) {
                if (costsForDegree[i].isSet(indexA, indexB)) {
                    return (byte) i;
                }
            }
            return MAX_VALUE;
        }

        public CostsForDegree costsFor(int currentDegree) {
            return costsForDegree[currentDegree];
        }
    }

    private static class RouteDateAndDayOverlap {
        // Create a bitmask corresponding to the dates and days routes overlap

        private final BitSet[] overlapMasks;
        private final int numberOfRoutes;
        private final Index index;

        private RouteDateAndDayOverlap(Index index, int numberOfRoutes) {
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
