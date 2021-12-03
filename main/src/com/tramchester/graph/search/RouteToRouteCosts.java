package com.tramchester.graph.search;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.caching.DataCache;
import com.tramchester.dataexport.DataSaver;
import com.tramchester.dataimport.data.RouteIndexData;
import com.tramchester.dataimport.data.RouteMatrixData;
import com.tramchester.domain.InterchangeStation;
import com.tramchester.domain.NumberOfChanges;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.filters.GraphFilter;
import com.tramchester.metrics.Timing;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@LazySingleton
public class RouteToRouteCosts implements BetweenRoutesCostRepository {
    private static final Logger logger = LoggerFactory.getLogger(RouteToRouteCosts.class);

    public final static String INDEX_FILE = "route_index.csv";
    public final static String ROUTE_MATRIX_FILE = "route_matrix.csv";

    public static final int MAX_DEPTH = 4;

    private final RouteRepository routeRepository;
    private final InterchangeRepository interchangeRepository;
    private final NeighboursRepository neighboursRepository;

    private final Costs costs;
    private final Index index;
    private final DataCache dataCache;
    private final GraphFilter graphFilter;

    @Inject
    public RouteToRouteCosts(RouteRepository routeRepository, InterchangeRepository interchangeRepository,
                             NeighboursRepository neighboursRepository, DataCache dataCache, GraphFilter graphFilter) {
        this.routeRepository = routeRepository;
        this.interchangeRepository = interchangeRepository;
        this.neighboursRepository = neighboursRepository;
        this.dataCache = dataCache;
        this.graphFilter = graphFilter;

        int numberOfRoutes = routeRepository.numberOfRoutes();
        index = new Index(numberOfRoutes);
        costs = new Costs(numberOfRoutes);
    }

   @PostConstruct
    public void start() {
        logger.info("starting");
        if (graphFilter.isFiltered()) {
           logger.warn("Filtering is enabled, skipping all caching");
           buildIndexAndCostMatrix();
        } else {
            if (dataCache.has(index) && dataCache.has(costs)) {
                dataCache.loadInto(index, RouteIndexData.class);
                dataCache.loadInto(costs, RouteMatrixData.class);
            } else {
                buildIndexAndCostMatrix();
                dataCache.save(index, RouteIndexData.class);
                dataCache.save(costs, RouteMatrixData.class);
            }
        }
        logger.info("started");
   }

   @PreDestroy
   public void stop() {
        logger.info("stopping");
        index.clear();
        logger.info("stopped");
   }

    private void buildIndexAndCostMatrix() {
        try (Timing ignored = new Timing(logger, "RouteToRouteCosts")) {
            index.populateFrom(routeRepository);
            RouteDateAndDayOverlap routeDateAndDayOverlap = new RouteDateAndDayOverlap(index);
            routeDateAndDayOverlap.populateFor(routeRepository);
            populateCosts(routeDateAndDayOverlap);
        }
    }

    private void populateCosts(RouteDateAndDayOverlap routeDateAndDayOverlap) {
        final int size = routeRepository.numberOfRoutes();
        logger.info("Find costs between " + size + " routes");
        final int fullyConnected = size * size;
        final double fullyConnectedPercentage = fullyConnected / 100D;

        // route -> [reachable routes]
        InterimResults linksForRoutes = addInitialConnectionsFromInterchanges();

        logger.info("Have " +  costs.size() + " connections from " + interchangeRepository.size() + " interchanges");

        // for existing connections infer next degree of connections, stop if no more added
        for (byte currentDegree = 1; currentDegree <= MAX_DEPTH; currentDegree++) {
            logger.info("Adding connections for degree " + currentDegree);
            // create new: route -> [reachable routes]
            final InterimResults newLinksForRoutes = addConnectionsFor(routeDateAndDayOverlap, currentDegree, linksForRoutes);
            if (newLinksForRoutes.isEmpty()) {
                logger.info("Finished at degree " + (currentDegree-1));
                linksForRoutes.clear();
                break;
            } else {
                logger.info("Total is " + costs.size() + " " + costs.size()/fullyConnectedPercentage +"%");
            }
            linksForRoutes = newLinksForRoutes;
        }

        final int finalSize = costs.size();
        logger.info("Added cost for " + finalSize + " route combinations");
        if (finalSize != fullyConnected) {
            logger.warn("Not fully connected, only " + finalSize + " of " + fullyConnected);
        }
    }

    private InterimResults addConnectionsFor(RouteDateAndDayOverlap routeDateAndDayOverlap, byte currentDegree,
                                             InterimResults currentlyReachableRoutes) {
        final Instant startTime = Instant.now();
        final byte nextDegree = (byte)(currentDegree+1);

        final InterimResults results = new InterimResults();
        for(Map.Entry<Integer, Set<Integer>> currentlyReachable : currentlyReachableRoutes.routeAndReachableRoutes()) {

            final int sourceRouteIndex = currentlyReachable.getKey();
            final Set<Integer> connectedRoutesIndexes = currentlyReachable.getValue(); // routes we can currently reach for current key

            final DateOverflatFilter overlapfilter = routeDateAndDayOverlap.createFilterFor(sourceRouteIndex);
            final AlreadyPresentFilter alreadyPresentFilter = costs.createFilterFor(sourceRouteIndex);

            final Set<Integer> newConnectionsForARoute =
                    connectedRoutesIndexes.stream().
                            filter(currentlyReachableRoutes::hasRoute).
                            flatMap(index -> currentlyReachableRoutes.getReachableRoutes(index).stream()).
                            parallel().
                            filter(routeIndex -> overlapfilter.overlaps(routeIndex) && alreadyPresentFilter.notPresent(routeIndex)).
                            collect(Collectors.toSet());

            if (!newConnectionsForARoute.isEmpty()) {
                results.put(sourceRouteIndex, newConnectionsForARoute);
            }
        }
        logger.info("Discover " + Duration.between(startTime, Instant.now()).toMillis() + " ms");

        final int before = costs.size();
        results.addRoutesToCosts(costs, nextDegree);
        final int added = costs.size() - before;

        final long took = Duration.between(startTime, Instant.now()).toMillis();
        logger.info("Added " + added + " extra connections for degree " + currentDegree + " in " + took + " ms");
        return results;
    }

    private InterimResults addInitialConnectionsFromInterchanges() {
        // seed connections between routes using interchanges
        final InterimResults linksForRoutes = new InterimResults();
        // same mode interchanges
        final Set<InterchangeStation> interchanges = interchangeRepository.getAllInterchanges();
        logger.info("Pre-populate route to route costs from " + interchanges.size() + " interchanges");
        interchanges.forEach(interchange -> addOverlapsFor(interchange, linksForRoutes));

        return linksForRoutes;
    }

    private void addOverlapsFor(InterchangeStation interchange, InterimResults interimResults) {

        final Set<Route> dropOffAtInterchange = interchange.getDropoffRoutes();
        final Set<Route> pickupAtInterchange = interchange.getPickupRoutes();

        for (final Route dropOff : dropOffAtInterchange) {
            final IdFor<Route> dropOffId = dropOff.getId();
            final int dropOffIndex = index.indexFor(dropOffId);

            for (final Route pickup : pickupAtInterchange) {
                if (!dropOff.equals(pickup)) {
                    final int pickupIndex = index.indexFor(pickup.getId());
                    if (dropOff.isDateOverlap(pickup)) {
                        if (!costs.contains(dropOffIndex, pickupIndex)) {
                            costs.put(dropOffIndex, pickupIndex, (byte) 1);
                            interimResults.add(dropOffIndex, pickupIndex);
                        }
                    }
                }
            }
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
    public NumberOfChanges getNumberOfChanges(Set<Station> starts, Set<Station> destinations) {
        if (areNeighbours(starts, destinations)) {
            return new NumberOfChanges(0, maxHops(pickupRoutesFor(starts), dropoffRoutesFor(destinations)));
        }
        return getNumberOfHops(pickupRoutesFor(starts), dropoffRoutesFor(destinations));
    }

    @Override
    public NumberOfChanges getNumberOfChanges(Station startStation, Station destination) {
        if (areNeighbours(startStation, destination)) {
            return new NumberOfChanges(0, maxHops(startStation.getPickupRoutes(), destination.getDropoffRoutes()));
        }
        return getNumberOfHops(startStation.getPickupRoutes(), destination.getDropoffRoutes());
    }

    @Override
    public LowestCostsForRoutes getLowestCostCalcutatorFor(Set<Station> destinations) {
        Set<Route> destinationRoutes = destinations.stream().
                map(Station::getDropoffRoutes).flatMap(Collection::stream).collect(Collectors.toUnmodifiableSet());
        return new LowestCostForDestinations(this, destinationRoutes);
    }

    private boolean areNeighbours(Station startStation, Station destination) {
        if (!neighboursRepository.hasNeighbours(startStation.getId())) {
            return false;
        }
        Set<Station> neighbours = neighboursRepository.getNeighboursFor(startStation.getId());
        return neighbours.contains(destination);
    }

    private boolean areNeighbours(Set<Station> starts, Set<Station> destinations) {
        return starts.stream().
                map(Station::getId).
                filter(neighboursRepository::hasNeighbours).
                map(neighboursRepository::getNeighboursFor).
                anyMatch(neighbours -> destinations.stream().anyMatch(neighbours::contains));
    }

    @NotNull
    private NumberOfChanges getNumberOfHops(Set<Route> startRoutes, Set<Route> destinationRoutes) {
        int minHops = minHops(startRoutes, destinationRoutes);
        int maxHops = maxHops(startRoutes, destinationRoutes);
        return new NumberOfChanges(minHops, maxHops);
    }

    private int minHops(Set<Route> startRoutes, Set<Route> endRoutes) {
        return startRoutes.stream().
                flatMap(startRoute -> endRoutes.stream().map(endRoute -> getFor(startRoute, endRoute))).
                min(Integer::compare).orElse(Integer.MAX_VALUE);
    }

    private Integer maxHops(Set<Route> startRoutes, Set<Route> endRoutes) {
        return startRoutes.stream().
                flatMap(startRoute -> endRoutes.stream().map(endRoute -> getFor(startRoute, endRoute))).
                filter(result -> result != Integer.MAX_VALUE).
                max(Integer::compare).orElse(Integer.MAX_VALUE);
    }

    private Set<Route> dropoffRoutesFor(Set<Station> stations) {
        return stations.stream().flatMap(station -> station.getDropoffRoutes().stream()).collect(Collectors.toSet());
    }

    private Set<Route> pickupRoutesFor(Set<Station> stations) {
        return stations.stream().flatMap(station -> station.getPickupRoutes().stream()).collect(Collectors.toSet());
    }

    private static class LowestCostForDestinations implements LowestCostsForRoutes {
        private final RouteToRouteCosts routeToRouteCosts;
        private final Set<Integer> destinationIndexs;

        public LowestCostForDestinations(BetweenRoutesCostRepository routeToRouteCosts, Set<Route> destinations) {
            this.routeToRouteCosts = (RouteToRouteCosts) routeToRouteCosts;
            destinationIndexs = destinations.stream().
                    map(destination -> this.routeToRouteCosts.index.indexFor(destination.getId())).
                    collect(Collectors.toUnmodifiableSet());
        }

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
                    filter(dest -> routeToRouteCosts.costs.contains(indexOfStart, dest)).
                    map(dest -> routeToRouteCosts.costs.get(indexOfStart, dest)).
                    min().
                    orElse(Integer.MAX_VALUE);
            return Pair.of(result, start);
        }

    }

    private static class Index implements DataCache.Cacheable<RouteIndexData> {
        private final Map<IdFor<Route>, Integer> mapRouteIdToIndex;
        private final Map<Integer, IdFor<Route>>  mapIndexToRouteId;
        private final int numberOfRoutes;

        private Index(int numberOfRoutes) {
            mapRouteIdToIndex = new HashMap<>(numberOfRoutes);
            mapIndexToRouteId = new HashMap<>(numberOfRoutes);
            this.numberOfRoutes = numberOfRoutes;
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
            stream.forEach(item -> mapRouteIdToIndex.put(item.getRouteId(), item.getIndex()));
            if (mapRouteIdToIndex.size()!=numberOfRoutes) {
                throw new DataCache.CacheLoadException("Mismatch on number of routes, got " + mapRouteIdToIndex.size() +
                        " expected " + numberOfRoutes);
            }
        }

        public IdFor<Route> getIdFor(int routeIndex) {
            return mapIndexToRouteId.get(routeIndex);
        }

        public void clear() {
            mapRouteIdToIndex.clear();
            mapIndexToRouteId.clear();
        }
    }

    private static class Costs implements DataCache.Cacheable<RouteMatrixData> {
        public static final byte MAX_VALUE = Byte.MAX_VALUE;

        private final byte[][] costArray;
        private final AtomicInteger count;
        private final int numRoutes;

        private Costs(int numRoutes) {
            count = new AtomicInteger(0);
            this.numRoutes = numRoutes;
            costArray = new byte[numRoutes][numRoutes];
            resetArray(numRoutes);
            //numberSetFor = new int[numRoutes];
        }

        private void resetArray(int size) {
            for (int i = 0; i < size; i++) {
                Arrays.fill(costArray[i], MAX_VALUE);
            }
        }

        public int size() {
            return count.get();
        }

        public boolean contains(int i, int j) {
            return costArray[i][j] != MAX_VALUE;
        }

        public void put(int indexA, int indexB, byte value) {
            count.incrementAndGet();
            costArray[indexA][indexB] = value;
        }

        public byte get(int a, int b) {
            return costArray[a][b];
        }

        @Override
        public void cacheTo(DataSaver<RouteMatrixData> saver) {
            List<RouteMatrixData> dataToSave = new ArrayList<>(numRoutes);
            for (int i = 0; i < numRoutes; i++) {
                byte[] row = costArray[i];
                List<Integer> destinations = new ArrayList<>(numRoutes);
                for (int j = 0; j < numRoutes; j++) {
                    destinations.add(j, (int) row[j]);
                }
                RouteMatrixData routeMatrixData = new RouteMatrixData(i, destinations);
                dataToSave.add(i, routeMatrixData);
            }
            saver.save(dataToSave);
            dataToSave.clear();
        }

        @Override
        public String getFilename() {
            return ROUTE_MATRIX_FILE;
        }

        @Override
        public void loadFrom(Stream<RouteMatrixData> stream) throws DataCache.CacheLoadException {
            AtomicInteger loadedOk = new AtomicInteger(0);

            stream.filter(item -> item.getDestinations().size()==numRoutes).
                    forEach(item -> {
                        int source = item.getSource();
                        List<Integer> dest = item.getDestinations();
                        for (int j = 0; j < numRoutes; j++) {
                            final byte value = dest.get(j).byteValue();
                            costArray[source][j] = value;
                            if (value != Byte.MAX_VALUE) {
                                count.incrementAndGet();
                            }
                        }
                        loadedOk.incrementAndGet();
                    });

            if (loadedOk.get() != numRoutes) {
                throw new DataCache.CacheLoadException("Could not load all rows, mismatch on number routes, loaded " +
                        loadedOk.get() + " expected " + numRoutes);
            }
        }

        public AlreadyPresentFilter createFilterFor(int sourceRouteIndex) {
            return new AlreadyPresentFilter(costArray[sourceRouteIndex], numRoutes);
        }
    }

    private static class InterimResults {
        private final Map<Integer, Set<Integer>> theMap;

        private InterimResults() {
            theMap = new TreeMap<>();
        }

        public void clear() {
            theMap.clear();
        }

        public boolean isEmpty() {
            return theMap.isEmpty();
        }

        public Iterable<? extends Map.Entry<Integer, Set<Integer>>> routeAndReachableRoutes() {
            return Collections.unmodifiableSet(theMap.entrySet());
        }

        // only storing new things to add here, hence the put
        public void put(int routeIndex, Set<Integer> routes) {
            theMap.put(routeIndex, routes);
        }

        public void addRoutesToCosts(Costs costs, byte degree) {
            // todo could optimise further by getting row first, then updating each element for that row
            theMap.forEach((route, dests) ->
                    dests.forEach(dest -> costs.put(route, dest, degree)));
        }

        public boolean hasRoute(int integer) {
            return theMap.containsKey(integer);
        }

        public void add(int dropOffIndex, int pickupIndex) {
            if (!theMap.containsKey(dropOffIndex)) {
                theMap.put(dropOffIndex, new HashSet<>());
            }
            theMap.get(dropOffIndex).add(pickupIndex);
        }

        public Set<Integer> getReachableRoutes(int integer) {
            return theMap.get(integer);
        }

    }

    private static class RouteDateAndDayOverlap {

        private final boolean[][] overlaps;
        private final Index index;

        private RouteDateAndDayOverlap(Index index) {
            this.index = index;
            overlaps = new boolean[index.numberOfRoutes][index.numberOfRoutes];
        }

        public void populateFor(RouteRepository repository) {
            logger.info("Creating matrix for route date/day overlap");
            int numberOfRoutes = index.numberOfRoutes;
            for (int i = 0; i < numberOfRoutes; i++) {
                for (int j = 0; j < numberOfRoutes; j++) {
                    if (i!=j) {
                        IdFor<Route> fromId = index.getIdFor(i);
                        IdFor<Route> toId = index.getIdFor(j);
                        Route from = repository.getRouteById(fromId);
                        Route to = repository.getRouteById(toId);
                        overlaps[i][j] = from.isDateOverlap(to);
                    }
                }
            }
            logger.info("Finished matrix for route date/day overlap");
        }

        public DateOverflatFilter createFilterFor(int sourceRouteIndex) {
            boolean[] row = overlaps[sourceRouteIndex];
            return new DateOverflatFilter(row);
        }
    }

    private static class DateOverflatFilter {
        private final boolean[] row;

        public DateOverflatFilter(boolean[] row) {
            this.row = row;
        }

        public boolean overlaps(int newRoute) {
            return row[newRoute];
        }
    }

    private static class AlreadyPresentFilter {
        private final boolean[] notPresent;

        public AlreadyPresentFilter(byte[] values, int size) {
            notPresent = new boolean[size];
            for (int i = 0; i < size; i++) {
                notPresent[i] = (values[i]==Costs.MAX_VALUE);
            }
        }

        public boolean notPresent(int newRoute) {
            return notPresent[newRoute];
        }
    }
}
