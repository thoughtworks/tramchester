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
import javax.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
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

    private void buildIndexAndCostMatrix() {
        try (Timing ignored = new Timing(logger, "RouteToRouteCosts")) {
            index.populateFrom(routeRepository);
            populateCosts();
        }
    }

    private void populateCosts() {
        List<Route> routes = new ArrayList<>(routeRepository.getRoutes());
        int size = routes.size();
        logger.info("Find costs between " + size + " routes");
        final int fullyConnected = size * size;
        double fullyConnectedPercentage = fullyConnected / 100D;

        // route -> [reachable routes]
        InterimResults linksForRoutes = addInitialConnectionsFromInterchanges();

        logger.info("Have " +  costs.size() + " connections from " + interchangeRepository.size() + " interchanges");

        // for existing connections infer next degree of connections, stop if no more added
        for (byte currentDegree = 1; currentDegree <= MAX_DEPTH; currentDegree++) {
            // create new: route -> [reachable routes]
            InterimResults newLinksForRoutes = addConnectionsFor(currentDegree, linksForRoutes);
            if (newLinksForRoutes.isEmpty()) {
                logger.info("Finished at degree " + (currentDegree-1));
                linksForRoutes.clear();
                break;
            } else {
                logger.info("Total is " + costs.size() + " " + costs.size()/fullyConnectedPercentage +"%");
            }
            //linksForRoutes.clear();
            linksForRoutes = newLinksForRoutes;
        }

        final int finalSize = costs.size();
        logger.info("Added cost for " + finalSize + " route combinations");
        if (finalSize != fullyConnected) {
            logger.warn("Not fully connected, only " + finalSize + " of " + fullyConnected);
        }
    }

    private InterimResults addConnectionsFor(byte currentDegree, InterimResults currentlyReachableRoutes) {
        Instant startTime = Instant.now();
        final byte nextDegree = (byte)(currentDegree+1);
        InterimResults additional = new InterimResults(); // discovered route -> [route] for this degree
        for(Map.Entry<Integer, Set<Integer>> entry : currentlyReachableRoutes.entrySet()) {

            final int routeIndex = entry.getKey();
            final Set<Integer> connectedToRoute = entry.getValue(); // routes we can currently reach for current key

            // for each reachable route, find the routes we can in turn reach from them not already found previous degree
            final Set<Integer> newConnections = connectedToRoute.parallelStream().
                    filter(currentlyReachableRoutes::containsKey).
                    map(currentlyReachableRoutes::get).
                    filter(routeSet -> !routeSet.isEmpty()).
                    map(routeSet -> costs.notAlreadyAdded(routeIndex, routeSet)).
                    flatMap(Collection::stream).
                    collect(Collectors.toSet());

            if (!newConnections.isEmpty()) {
                additional.put(routeIndex, newConnections);
            }
        }
        logger.info("Discover " + Duration.between(startTime, Instant.now()).toMillis() + " ms");

        int before = costs.size();
        additional.addTo(costs, nextDegree);
        final int added = costs.size() - before;

        long took = Duration.between(startTime, Instant.now()).toMillis();
        logger.info("Added " + added + " extra connections for degree " + currentDegree + " in " + took + " ms");
        return additional;
    }

    private InterimResults addInitialConnectionsFromInterchanges() {
        // seed connections between routes using interchanges
        InterimResults linksForRoutes = new InterimResults();
        // same mode interchanges
        interchangeRepository.getAllInterchanges().
                forEach(station -> addOverlapsFor(station, linksForRoutes));

        return linksForRoutes;
    }

    private void addOverlapsFor(InterchangeStation interchange, InterimResults linksForDegree) {

        List<IdFor<Route>> source = interchange.getSourceRoutes().stream().map(Route::getId).collect(Collectors.toList());
        int sourceSize = source.size();
        List<IdFor<Route>> dest = interchange.getDestinationRoutes().stream().map(Route::getId).collect(Collectors.toList());
        int destSize = dest.size();

        for (int i = 0; i < sourceSize; i++) {
            final IdFor<Route> fromId = source.get(i);
            final int from = index.find(fromId);
            if (!linksForDegree.containsKey(from)) {
                linksForDegree.put(from, new HashSet<>());
            }
            for (int j = 0; j < destSize; j++) {
                if (i != j) {
                    final IdFor<Route> towardsId = dest.get(j);
                    final int towards = index.find(towardsId);
                    linksForDegree.get(from).add(towards);
                    if (!costs.contains(from, towards)) {

                        costs.put(from, towards, (byte) 1);
                    }
                }
            }
        }
    }

    public int getFor(Route routeA, Route routeB) {
        if (routeA.equals(routeB)) {
            return 0;
        }
        final IdFor<Route> idA = routeA.getId();
        final IdFor<Route> idB = routeB.getId();
        byte result = costs.get(index.find(idA), index.find(idB));
        if (result==Costs.MAX_VALUE) {
            if (routeA.getTransportMode()==routeB.getTransportMode()) {
                // for mixed transport mode having no value is quite normal
                final String msg = "Missing (routeId:" + idA + ", routeId:" + idB + ")";
                logger.warn(msg);
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
            return new NumberOfChanges(0, maxHops(routesFor(starts), routesFor(destinations)));
        }
        return getNumberOfHops(routesFor(starts), routesFor(destinations));
    }

    @Override
    public NumberOfChanges getNumberOfChanges(Station startStation, Station destination) {
        if (areNeighbours(startStation, destination)) {
            return new NumberOfChanges(0, maxHops(startStation.getRoutes(), destination.getRoutes()));
        }
        return getNumberOfHops(startStation.getRoutes(), destination.getRoutes());
    }

    @Override
    public LowestCostsForRoutes getLowestCostCalcutatorFor(Set<Station> destinations) {
        Set<Route> destinationRoutes = destinations.stream().
                map(Station::getRoutes).flatMap(Collection::stream).collect(Collectors.toUnmodifiableSet());
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
                filter(result -> result!=Integer.MAX_VALUE).
                max(Integer::compare).orElse(Integer.MAX_VALUE);
    }

    private Set<Route> routesFor(Set<Station> stations) {
        return stations.stream().flatMap(station -> station.getRoutes().stream()).collect(Collectors.toSet());
    }

    private static class LowestCostForDestinations implements LowestCostsForRoutes {
        private final RouteToRouteCosts routeToRouteCosts;
        private final Set<Integer> destinationIndexs;

        public LowestCostForDestinations(BetweenRoutesCostRepository routeToRouteCosts, Set<Route> destinations) {
            this.routeToRouteCosts = (RouteToRouteCosts) routeToRouteCosts;
            destinationIndexs = destinations.stream().
                    map(destination -> this.routeToRouteCosts.index.find(destination.getId())).
                    collect(Collectors.toUnmodifiableSet());
        }

        @Override
        public int getFewestChanges(Route startingRoute) {
            int indexOfStart = routeToRouteCosts.index.find(startingRoute.getId());
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
            int indexOfStart = routeToRouteCosts.index.find(start.getId());
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
        private final Map<IdFor<Route>, Integer> map;
        private final int numberOfRoutes;

        private Index(int numberOfRoutes) {
            map = new HashMap<>(numberOfRoutes);
            this.numberOfRoutes = numberOfRoutes;
        }

        public void populateFrom(RouteRepository routeRepository) {
            logger.info("Creating index");
            List<IdFor<Route>> routesList = routeRepository.getRoutes().stream().map(Route::getId).collect(Collectors.toList());
            createIndex(routesList);
            logger.info("Added " + map.size() +" index entries");
        }

        private void createIndex(List<IdFor<Route>> routesList) {
            for (int i = 0; i < routesList.size(); i++) {
                map.put(routesList.get(i), i);
            }
        }

        public int size() {
            return map.size();
        }

        public int find(IdFor<Route> from) {
            return map.get(from);
        }

        public IdFor<Route> find(int index) {
            return map.entrySet().stream().
                    filter(entry -> entry.getValue()==index).
                    map(Map.Entry::getKey).
                    findFirst().orElseThrow();
        }

        @Override
        public void cacheTo(DataSaver<RouteIndexData> saver) {
            List<RouteIndexData> indexData = map.entrySet().stream().
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
            stream.forEach(item -> map.put(item.getRouteId(), item.getIndex()));
            if (map.size()!=numberOfRoutes) {
                throw new DataCache.CacheLoadException("Mismatch on number of routes, got " + map.size() +
                        " expected " + numberOfRoutes);
            }
        }
    }

    private static class Costs implements DataCache.Cacheable<RouteMatrixData> {
        public static final byte MAX_VALUE = Byte.MAX_VALUE;

        private final byte[][] array;
        private final int[] numberSetFor;
        private final AtomicInteger count;
        private final int numRoutes;

        private Costs(int numRoutes) {
            count = new AtomicInteger(0);
            this.numRoutes = numRoutes;
            array = new byte[numRoutes][numRoutes];
            resetArray(numRoutes);
            numberSetFor = new int[numRoutes];
        }

        private void resetArray(int size) {
            for (int i = 0; i < size; i++) {
                Arrays.fill(array[i], MAX_VALUE);
            }
        }

        public int size() {
            return count.get();
        }

        public boolean contains(int i, int j) {
            return array[i][j] != MAX_VALUE;
        }

        public void put(int indexA, int indexB, byte value) {
            count.incrementAndGet();
            array[indexA][indexB] = value;
            numberSetFor[indexA]++;
        }

        public byte get(int a, int b) {
            return array[a][b];
        }

        // Collectors.toList marginally faster, parallelStream slower
        public List<Integer> notAlreadyAdded(int routeIndex, Set<Integer> routeSet) {
            if (numberSetFor[routeIndex] == numRoutes) {
                return Collections.emptyList();
            }
            final byte[] forIndex = array[routeIndex]; // get row for current routeIndex
            return routeSet.stream().filter(present(forIndex)).collect(Collectors.toList());
        }

        @NotNull
        private Predicate<Integer> present(byte[] forIndex) {
            return found -> forIndex[found] == MAX_VALUE;
        }

        @Override
        public void cacheTo(DataSaver<RouteMatrixData> saver) {
            List<RouteMatrixData> dataToSave = new ArrayList<>(numRoutes);
            for (int i = 0; i < numRoutes; i++) {
                byte[] row = array[i];
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
                            array[source][j] = value;
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
    }

    private static class InterimResults {
        private final Map<Integer, Set<Integer>> theMap;

        private InterimResults() {
            theMap = new TreeMap<>();
        }

        public boolean containsKey(int key) {
            return theMap.containsKey(key);
        }

        public void clear() {
            theMap.clear();
        }

        public boolean isEmpty() {
            return theMap.isEmpty();
        }

        public Set<Map.Entry<Integer, Set<Integer>>> entrySet() {
            return Collections.unmodifiableSet(theMap.entrySet());
        }

        public Set<Integer> get(int routeIndex) {
            final Set<Integer> result = theMap.get(routeIndex);
            if (result==null) {
                throw new RuntimeException("Got null entry for index " + routeIndex+ " map " + theMap);
            }
            return result;
        }

        // only storing new things to add here, hence the put
        public void put(int routeIndex, Set<Integer> routes) {
            theMap.put(routeIndex, routes);
        }

        public void addTo(Costs costs, byte degree) {
            // todo could optimise further by getting row first, then updating each element for that row
            theMap.forEach((key, dests) ->
                    dests.forEach(dest -> costs.put(key, dest, degree)));
        }
    }

}
