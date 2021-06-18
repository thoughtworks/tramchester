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
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@LazySingleton
public class RouteToRouteCosts implements BetweenRoutesCostRepository {
    private static final Logger logger = LoggerFactory.getLogger(RouteToRouteCosts.class);
    public static final int DEPTH = 4;

    private final RouteRepository routeRepository;
    private final InterchangeRepository interchangeRepository;

    // size is a real issue here, > 2M entries for buses
    private final Map<Key, Byte> costs;

    @Inject
    public RouteToRouteCosts(RouteRepository routeRepository, InterchangeRepository interchangeRepository) {
        this.routeRepository = routeRepository;
        this.interchangeRepository = interchangeRepository;
        costs = new HashMap<>();
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
        for(Map.Entry<IdFor<Route>, IdSet<Route>> entry : currentlyReachableRoutes.entrySet()) {

            final IdFor<Route> routeId = entry.getKey();
            IdSet<Route> connectedToRoute = entry.getValue(); // routes we can currently reach for current key

            // for each reachable route, find the routes we can in turn reach from them not already found previous degree
            IdSet<Route> newConnections = connectedToRoute.parallelStream().
                    map(currentlyReachableRoutes::get).
                    filter(routeSet -> !routeSet.isEmpty()).
                    map(routeSet ->
                            routeSet.stream().filter(found -> !costs.containsKey(new Key(routeId, found))).collect(Collectors.toSet())
                    ).
                    flatMap(Collection::stream).
                    collect(IdSet.idCollector());

            if (!newConnections.isEmpty()) {
                additional.put(routeId, newConnections);
            }
        }
        logger.info("Discover " + Duration.between(startTime, Instant.now()).toMillis() + " ms");

        // filter based on not already present
        Stream<Key> keysToAdd = additional.keySet().stream().
                flatMap(route -> additional.get(route).stream().map(newConnection -> new Key(route, newConnection)));

        int before = costs.size();
        keysToAdd.forEach(key -> costs.put(key, nextDegree));
        if (costs.size()==before) {
            additional.clear();
        }

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
            final IdFor<Route> from = list.get(i);
            if (!linksForDegree.containsKey(from)) {
                linksForDegree.put(from, new IdSet<>());
            }
            for (int j = 0; j < size; j++) {
                if (i != j) {
                    final IdFor<Route> towards = list.get(j);
                    linksForDegree.get(from).add(towards);
                    Key key = new Key(from, towards);
                    if (!costs.containsKey(key)) {
                        costs.put(key, (byte) 1);
                    }
                }
            }
        }
    }

    public int getFor(Route routeA, Route routeB) {
        if (routeA.equals(routeB)) {
            return 0;
        }
        Key key = new Key(routeA, routeB);
        if (!costs.containsKey(key)) {
            final String msg = "Missing key " + key;
            logger.warn(msg);
            return Integer.MAX_VALUE;
        }
        return costs.get(key);
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
        IdFor<Route> start = hasStartId.getId();
        if (destinations.contains(start)) {
            return Pair.of((byte)0, hasStartId); // start on route that is present at destination
        }

        byte result = destinations.stream().map(destination -> new Key(start, destination)).
                filter(costs::containsKey).
                map(costs::get).
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
        private final Map<IdFor<Route>, IdSet<Route>> theMap;

        private InterimResults() {
            theMap = new TreeMap<>();
        }

        public boolean containsKey(IdFor<Route> key) {
            return theMap.containsKey(key);
        }

        public void clear() {
            theMap.clear();
        }

        public boolean isEmpty() {
            return theMap.isEmpty();
        }

        public Iterable<? extends Map.Entry<IdFor<Route>, IdSet<Route>>> entrySet() {
            return theMap.entrySet();
        }

        public IdSet<Route> get(IdFor<Route> routeId) {
            return theMap.get(routeId);
        }

        public void put(IdFor<Route> routeId, IdSet<Route> idSet) {
            theMap.put(routeId, idSet);
        }

        public Set<IdFor<Route>> keySet() {
            return theMap.keySet();
        }
    }

    private static class Key implements Comparable<Key> {

        private final IdFor<Route> first;
        private final IdFor<Route> second;
        private final int hashCode;

        public Key(Route routeA, Route routeB) {
            this(routeA.getId(), routeB.getId());

        }

        public Key(IdFor<Route> first, IdFor<Route> second) {
            this.first = first;
            this.second = second;
            this.hashCode = Key.createHashCode(this);
        }

        private static int createHashCode(Key key) {
            int result = key.first.hashCode();
            result = 31 * result + key.second.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (!first.equals(key.first)) return false;
            return second.equals(key.second);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public String toString() {
            return "Key{" +
                    "first=" + first +
                    ", second=" + second +
                    '}';
        }

        @Override
        public int compareTo(@NotNull RouteToRouteCosts.Key o) {
            int initial = this.first.compareTo(o.first);
            if (initial!=0) {
                return initial;
            }
            return this.second.compareTo(o.second);
        }
    }

}
