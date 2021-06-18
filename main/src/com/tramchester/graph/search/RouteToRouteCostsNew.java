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
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@LazySingleton
public class RouteToRouteCostsNew implements BetweenRoutesCostRepository {
    private static final Logger logger = LoggerFactory.getLogger(RouteToRouteCostsNew.class);
    public static final int DEPTH = 4;

    private final RouteRepository routeRepository;
    private final InterchangeRepository interchangeRepository;

    // size is a real issue here, > 2M entries for buses
    private final Map<IdFor<Route>, RouteHops> costs;

    @Inject
    public RouteToRouteCostsNew(RouteRepository routeRepository, InterchangeRepository interchangeRepository) {
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
        Map<IdFor<Route>, RouteHops> linksForRoutes = addInitialConnectionsFromInterchanges();

        logger.info("Have " +  costs.size() + " connections from " + interchangeRepository.size() + " interchanges");

        // for existing connections infer next degree of connections, stop if no more added
        for (byte currentDegree = 1; currentDegree <= DEPTH; currentDegree++) {
            // create new: route -> [reachable routes]
            Map<IdFor<Route>, RouteHops> newLinksForRoutes = addConnectionsFor(currentDegree, linksForRoutes);
            if (newLinksForRoutes.isEmpty()) {
                logger.info("Finished at degree " + (currentDegree-1));
                linksForRoutes.clear();
                break;
            } else {
                logger.info("Total is " + size() + " " + size()/fullyConnectedPercentage +"%");
            }
            linksForRoutes.clear();
            linksForRoutes = newLinksForRoutes;
        }

        final int finalSize = size();
        logger.info("Added cost for " + finalSize + " route combinations");
        if (finalSize != fullyConnected) {
            logger.warn("Not fully connected, only " + finalSize + " of " + fullyConnected);
        }
    }

    private Map<IdFor<Route>, RouteHops> addConnectionsFor(byte currentDegree, Map<IdFor<Route>, RouteHops> currentlyReachableRoutes) {
        Instant startTime = Instant.now();
        final byte nextDegree = (byte)(currentDegree+1);
        int added = 0;

        final Map<IdFor<Route>, RouteHops> additional = new ConcurrentHashMap<>(); // discovered route -> [route] for this degree
        for(Map.Entry<IdFor<Route>, RouteHops> entry : currentlyReachableRoutes.entrySet()) {

            final IdFor<Route> routeId = entry.getKey();
            final RouteHops connectedToRoute = entry.getValue(); // routes we can currently reach for current key

            // for each reachable route, find the routes we can in turn reach from them not already found previous degree
            Set<IdSet<Route>> routeIdsToAdd = connectedToRoute.getRoutesIds().stream().parallel().
                    map(currentlyReachableRoutes::get).
                    filter(hops -> !hops.isEmpty()).
                    map(hops -> routeIdsNotAlreadyLinked(routeId, hops)).
                    filter(hops -> !hops.isEmpty()).
                    collect(Collectors.toSet());

            IdSet<Route> newConnections = routeIdsToAdd.stream().flatMap(IdSet::stream).collect(IdSet.idCollector());

            if (!newConnections.isEmpty()) {
                additional.put(routeId, new RouteHops(newConnections, nextDegree));
                added = added + newConnections.size();
            }
        }
        logger.info("Discover " + Duration.between(startTime, Instant.now()).toMillis() + " ms");

        additional.forEach((id, hops) -> getOrCreate(id).addAll(hops));

        long took = Duration.between(startTime, Instant.now()).toMillis();
        logger.info("Added " + added + " extra connections for degree " + currentDegree + " in " + took + " ms");
        return additional;
    }

    private IdSet<Route> routeIdsNotAlreadyLinked(IdFor<Route> routeId, RouteHops hops) {
        if (!costs.containsKey(routeId)) {
            return hops.getRoutesIds();
        }
        RouteHops currentHops = costs.get(routeId);
        return currentHops.routeIdsNotOverlapingWith(hops);
    }

    private RouteHops getOrCreate(IdFor<Route> key) {
        if (!costs.containsKey(key)) {
            costs.put(key, new RouteHops());
        }
        return costs.get(key);
    }

    private Map<IdFor<Route>, RouteHops> addInitialConnectionsFromInterchanges() {
        // seed connections between routes using interchanges
        Map<IdFor<Route>, RouteHops> linksForRoutes = new HashMap<>();
        interchangeRepository.getAllInterchanges().forEach(interchange -> addOverlapsFor(interchange, linksForRoutes, (byte)1));
        return linksForRoutes;
    }

    private void addOverlapsFor(Station interchange, Map<IdFor<Route>, RouteHops> linksForDegree, byte degree) {
        List<Route> list = new ArrayList<>(interchange.getRoutes());
        int size = list.size();
        for (int i = 0; i < size; i++) {
            final IdFor<Route> from = list.get(i).getId();
            if (!linksForDegree.containsKey(from)) {
                linksForDegree.put(from, new RouteHops());
            }
            for (int j = 0; j < size; j++) {
                if (i != j) {
                    final IdFor<Route> towardsId = list.get(j).getId();
                    linksForDegree.get(from).add(towardsId, degree);
                    getOrCreate(from).add(towardsId, degree);
                }
            }
        }
    }

    @Override
    public int getFor(Route routeA, Route routeB) {
        final IdFor<Route> idA = routeA.getId();
        if (routeA.equals(routeB)) {
            return 0;
        }
        if (costs.containsKey(idA)) {
            RouteHops hops = costs.get(idA);
            if (hops.contains(routeB.getId())) {
                return hops.getFor(routeB.getId());
            }
        }
        logger.warn("Not hops found between " + routeA.getId() + " and " + routeB.getId());
        return Integer.MAX_VALUE;
    }

    public int size() {
        return costs.values().stream().map(RouteHops::size).reduce(Integer::sum).orElseThrow();
    }

    @Override
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

        if (!costs.containsKey(hasStartId.getId())) {
            logger.info("Missing route hops for " + hasStartId.getId());
            return Pair.of(Byte.MAX_VALUE, hasStartId);
        }

        RouteHops hopsFrom = costs.get(hasStartId.getId());

        byte result = destinations.stream().
                filter(hopsFrom::contains).
                map(hopsFrom::getFor).
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

    private static class RouteHops {
        private final Map<IdFor<Route>, Byte> theMap;

        public RouteHops() {
            theMap = new TreeMap<>();
        }

        public RouteHops(IdSet<Route> newConnections, byte nextDegree) {
            theMap = newConnections.stream().collect(Collectors.toMap(item->item, item -> nextDegree));
        }

        public int size() {
            return theMap.size();
        }

        public void add(IdFor<Route> routeId, byte degree) {
            theMap.put(routeId, degree);
        }

        public boolean isEmpty() {
            return theMap.isEmpty();
        }

        @NotNull
        public Set<IdFor<Route>> getIds() {
            return theMap.keySet();
        }

        public IdSet<Route> routeIdsNotOverlapingWith(RouteHops others) {

            return others.getRoutesIds().stream().
                    filter(routeId -> !theMap.containsKey(routeId)).collect(IdSet.idCollector());
//            return new IdSet<>(Sets.difference(others.theMap.keySet(), this.theMap.keySet()));
        }

        public void addAll(RouteHops hops) {
            theMap.putAll(hops.theMap);
        }

        public boolean contains(IdFor<Route> id) {
            return theMap.containsKey(id);
        }

        public byte getFor(IdFor<Route> id) {
            return theMap.get(id);
        }

        public IdSet<Route> getRoutesIds() {
            return new IdSet<>(theMap.keySet());
        }
    }

}
