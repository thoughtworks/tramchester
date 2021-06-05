package com.tramchester.graph.search;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Route;
import com.tramchester.domain.StationPair;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.RouteCostCalculator;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.RouteRepository;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@LazySingleton
public class RouteToRouteCosts {
    private static final Logger logger = LoggerFactory.getLogger(RouteToRouteCosts.class);

    private final RouteCostCalculator routeCostCalculator;
    private final RouteRepository routeRepository;
    private final InterchangeRepository interchangeRepository;
    private final GraphDatabase graphDatabase;

    private final Map<Key, Long> costs;

    @Inject
    public RouteToRouteCosts(RouteCostCalculator routeCostCalculator, RouteRepository routeRepository,
                             InterchangeRepository interchangeRepository, GraphDatabase graphDatabase) {
        this.routeCostCalculator = routeCostCalculator;
        this.routeRepository = routeRepository;
        this.interchangeRepository = interchangeRepository;
        this.graphDatabase = graphDatabase;
        costs = new HashMap<>();
    }

   @PostConstruct
    public void start() {
        logger.info("starting");
        populateCosts();
        logger.info("started");
   }

    private void populateCosts() {
        List<Route> routes = new ArrayList<>(routeRepository.getRoutes());
        int size = routes.size();
        logger.info("Find costs between " + size + " routes");

        // seed connections via interchanges
        Set<Set<Route>> connected = interchangeRepository.getAllInterchanges().stream().
                map(Station::getRoutes).collect(Collectors.toSet());
        connected.forEach(connection -> addOverlaps(connection, 1L));

        logger.info("Have " +  costs.size() + " connections from " + interchangeRepository.size() + " interchanges");

        for (int currentDegree = 1; currentDegree < 6; currentDegree++) {
            if (addConnectionsFor(currentDegree)==0) {
                break;
            }
        }

        logger.info("Added cost for " + costs.size() + " route combinations");
    }

    private int addConnectionsFor(int currentDegree) {
        int before = costs.size();
        final long nextDegree = Integer.toUnsignedLong(currentDegree + 1);

        Map<Route, Set<Route>> connectionsForEachRoute = new HashMap<>();
        getEntriesForCost(currentDegree).forEach(entry -> {
            final Route route = entry.getKey().first();
            if (!connectionsForEachRoute.containsKey(route)) {
                connectionsForEachRoute.put(route, new HashSet<>());
            }
            connectionsForEachRoute.get(route).add(entry.getKey().second());
        });

        for(Route route : connectionsForEachRoute.keySet()) {
            Set<Route> connectedToRoute = connectionsForEachRoute.get(route);
            connectedToRoute.forEach(connectedRoute -> {
                Set<Route> connectedToConnected = connectionsForEachRoute.get(connectedRoute);
                connectedToConnected.forEach(possibleNextDegree -> {
                    Key key = new Key(route, possibleNextDegree);
                    if (!costs.containsKey(key)) {
                        costs.put(key, nextDegree);
                    }
                });
            });
        }
        connectionsForEachRoute.clear();
        final int added = costs.size() - before;
        logger.info("Added " + added + " extra connections for degree " + currentDegree);
        return added;
    }

    @NotNull
    private Stream<Map.Entry<Key, Long>> getEntriesForCost(int cost) {
        return costs.entrySet().stream().filter(entry -> entry.getValue() == cost);
    }

    private void addOverlaps(Set<Route> routes, long cost) {
        List<Route> list = new ArrayList<>(routes);
        int size = list.size();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (i != j) {
                    Key key = new Key(list.get(i), list.get(j));
                    if (!costs.containsKey(key)) {
                        costs.put(key, cost);
                    }
                }
            }
        }
    }

    private void findCheapest(Transaction txn, Route routeA, Route routeB) {
        Key key = new Key(routeA, routeB);
        Set<Station> starts = interchangeRepository.getInterchangesOn(routeA);
        Set<Station> ends = interchangeRepository.getInterchangesOn(routeB);
        if (overlaps(starts,ends)) {
            costs.put(key,1L);
        }

        logger.info("Find cost from " + routeA.getId() + " to " + routeB.getId() + " for " + starts.size()* ends.size()
                + " interchanges");
        Optional<Long> maybeCost = StationPair.combinationsOf(starts, ends).
                map(pair -> routeCostCalculator.getNumberHops(txn, pair.getBegin(), pair.getEnd())).
                min(Comparator.comparingLong(item -> item));
        maybeCost.ifPresent(cost -> costs.put(key, cost));
        if (maybeCost.isEmpty()) {
            logger.info("No routing found between " + routeA.getId() + " to " + routeB.getId());
        }
    }

    private boolean overlaps(Set<Station> setA, Set<Station> setB) {
        for(Station a : setA) {
            if (setB.contains(a)) {
                return true;
            }
        }
        for(Station b : setB) {
            if (setA.contains(b)) {
                return true;
            }
        }
        return false;
    }

    public long getFor(Route routeA, Route routeB) {
        if (routeA.equals(routeB)) {
            return 0;
        }
        Key key = new Key(routeA, routeB);
        return costs.get(key);
    }

    private static class Key {

        private final Route first;
        private final Route second;

        public Key(Route routeA, Route routeB) {
            this.first = routeA;
            this.second = routeB;
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
            int result = first.hashCode();
            result = 31 * result + second.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "Key{" +
                    "first=" + first.getId() +
                    ", second=" + second.getId() +
                    '}';
        }

        public Route first() {
            return first;
        }

        public Route second() {
            return second;
        }
    }
}
