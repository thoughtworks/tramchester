package com.tramchester.graph.search;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.metrics.Timing;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.RouteRepository;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@LazySingleton
public class RouteToRouteCosts implements BetweenRoutesCostRepository {
    private static final Logger logger = LoggerFactory.getLogger(RouteToRouteCosts.class);
    public static final int DEPTH = 4;

    private final RouteRepository routeRepository;
    private final InterchangeRepository interchangeRepository;

    private final Costs costs;
    private final Index index;

    @Inject
    public RouteToRouteCosts(RouteRepository routeRepository, InterchangeRepository interchangeRepository) {
        this.routeRepository = routeRepository;
        this.interchangeRepository = interchangeRepository;
        index = new Index(routeRepository);
        costs = new Costs(index);
    }

   @PostConstruct
    public void start() {
        logger.info("starting");
        try (Timing ignored = new Timing(logger, "RouteToRouteCosts")) {
           populateCosts();
       }
        logger.info("started");
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
        for (byte currentDegree = 1; currentDegree <= DEPTH; currentDegree++) {
            // create new: route -> [reachable routes]
            InterimResults newLinksForRoutes = addConnectionsFor(currentDegree, linksForRoutes);
            if (newLinksForRoutes.isEmpty()) {
                logger.info("Finished at degree " + (currentDegree-1));
                linksForRoutes.clear();
                break;
            } else {
                logger.info("Total is " + costs.size() + " " + costs.size()/fullyConnectedPercentage +"%");
            }
            linksForRoutes.clear();
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

            final int routeId = entry.getKey();
            Set<Integer> connectedToRoute = entry.getValue(); // routes we can currently reach for current key

            // for each reachable route, find the routes we can in turn reach from them not already found previous degree
            Set<Integer> newConnections = connectedToRoute.parallelStream().
                    map(currentlyReachableRoutes::get).
                    filter(routeSet -> !routeSet.isEmpty()).
                    map(routeSet -> routeSet.stream().filter(found -> !costs.contains(routeId, found)).collect(Collectors.toSet())).
                    flatMap(Collection::stream).
                    collect(Collectors.toSet());

            if (!newConnections.isEmpty()) {
                additional.put(routeId, newConnections);
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
        interchangeRepository.getAllInterchanges().forEach(interchange -> addOverlapsFor(interchange, linksForRoutes));
        return linksForRoutes;
    }

    private void addOverlapsFor(Station interchange, InterimResults linksForDegree) {
        List<IdFor<Route>> list = interchange.getRoutes().stream().map(Route::getId).collect(Collectors.toList());
        int size = list.size();
        for (int i = 0; i < size; i++) {
            final IdFor<Route> fromId = list.get(i);
            final int from = index.find(fromId);
            if (!linksForDegree.containsKey(from)) {
                linksForDegree.put(from, new HashSet<>());
            }
            for (int j = 0; j < size; j++) {
                if (i != j) {
                    final IdFor<Route> towardsId = list.get(j);
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
            return Integer.MAX_VALUE;
        }
        return result;
    }

    public int size() {
        return costs.size();
    }

    public <T extends HasId<Route>> Stream<T> sortByDestinations(Stream<T> startingRoutes, IdSet<Route> destinationRouteIds) {
        return startingRoutes.
                map(start -> findLowestCost(start, destinationRouteIds)).
                sorted(comparingByte(Pair::getLeft)).
                map(Pair::getRight);
    }

    private <T extends HasId<Route>> Pair<Byte, T> findLowestCost(T hasStartId, IdSet<Route> destinations) {
        IdFor<Route> startId = hasStartId.getId();
        if (destinations.contains(startId)) {
            return Pair.of((byte)0, hasStartId); // start on route that is present at destination
        }

        int start = index.find(startId);
        byte result = destinations.stream().//map(destination -> new Key(start, destination)).
                map(index::find).
                filter(dest -> costs.contains(start, dest)).
                map(dest -> costs.get(start, dest)).
                min(comparingByte(a -> a)).orElse(Byte.MAX_VALUE);
        return Pair.of(result, hasStartId);
    }

    public static <T> Comparator<T> comparingByte(ToByteFunction<? super T> keyExtractor) {
        return (Comparator<T> & Serializable)
                (c1, c2) -> Byte.compare(keyExtractor.applyAsByte(c1), keyExtractor.applyAsByte(c2));
    }

    @FunctionalInterface
    public interface ToByteFunction<T> {
        byte applyAsByte(T value);
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
            return theMap.entrySet();
        }

        public Set<Integer> get(int routeIndex) {
            return theMap.get(routeIndex);
        }

        public void put(int routeIndex, Set<Integer> routes) {
            theMap.put(routeIndex, routes);
        }

        public Set<Integer> keySet() {
            return theMap.keySet();
        }

        public void addTo(Costs costs, byte degree) {
            theMap.forEach((key, dests) -> dests.forEach(dest -> costs.put(key, dest, degree)));
        }
    }

    private static class Index {
        private final Map<IdFor<Route>, Integer> map;

        private Index(RouteRepository routeRepository) {
            List<IdFor<Route>> routesList = routeRepository.getRoutes().stream().map(Route::getId).collect(Collectors.toList());
            int size = routesList.size();
            map = new HashMap<>(size);
            createIndex(routesList);
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
    }

    private static class Costs {
        public static final byte MAX_VALUE = Byte.MAX_VALUE;

        private final byte[][] array;
        private final AtomicInteger count;
        private final Index index;

        private Costs(Index index) {
            this.index = index;
            count = new AtomicInteger(0);
            int size = index.size();
            array = new byte[size][size];
            resetArray(size);
        }

        private void resetArray(int size) {
            for (int i = 0; i < size; i++) {
                Arrays.fill(array[i], MAX_VALUE);
            }
        }

        public int size() {
            return count.get();
        }

//        @Deprecated
//        public boolean contains(IdFor<Route> idA, IdFor<Route> idB) {
//            int i = index.get(idA);
//            int j = index.get(idB);
//            return array[i][j] != MAX_VALUE;
//        }

        public boolean contains(int i, int j) {
            return array[i][j] != MAX_VALUE;
        }

//        @Deprecated
//        public void put(IdFor<Route> idA, IdFor<Route> idB, byte value) {
//            int i = index.get(idA);
//            int j = index.get(idB);
//            put(i, j, value);
//        }

        public void put(int indexA, int indexB, byte value) {
            count.incrementAndGet();
            array[indexA][indexB] = value;
        }

        public byte get(int a, int b) {
            final byte value = array[a][b];
            if (value == MAX_VALUE) {
                final String msg = "Missing (" + index.find(a) + ", " + index.find(b) +")";
                logger.warn(msg);
//                return Integer.MAX_VALUE;
            }
            return value;
        }
    }

}
