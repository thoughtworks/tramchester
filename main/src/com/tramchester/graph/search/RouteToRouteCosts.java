package com.tramchester.graph.search;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.RouteRepository;
import org.apache.commons.lang3.tuple.Pair;
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
    public static final int DEPTH = 3;

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
        populateCosts();
        logger.info("started");
   }

    private void populateCosts() {
        List<Route> routes = new ArrayList<>(routeRepository.getRoutes());
        int size = routes.size();
        logger.info("Find costs between " + size + " routes");

        Map<Route, Set<Route>> linksForRoutes = addInitialConnectionsFromInterchanges();

        logger.info("Have " +  costs.size() + " connections from " + interchangeRepository.size() + " interchanges");

        // for existing connections infer next degree of connections, stop if no more added
        for (byte currentDegree = 1; currentDegree < DEPTH; currentDegree++) {
            Map<Route, Set<Route>> newLinks = addConnectionsFor(currentDegree, linksForRoutes);
            if (newLinks.isEmpty()) {
                linksForRoutes.clear();
                break;
            }
            linksForRoutes.clear();
            linksForRoutes = newLinks;
        }

        logger.info("Added cost for " + costs.size() + " route combinations");
    }

    private Map<Route, Set<Route>> addConnectionsFor(byte currentDegree, Map<Route, Set<Route>> linksPreviousDegree) {
        Map<Route, Set<Route>> links = new HashMap<>();
        final byte nextDegree = (byte)(currentDegree+1);

        for(Map.Entry<Route, Set<Route>> entry : linksPreviousDegree.entrySet()) {
            Set<Route> connectedToRoute = entry.getValue();

            Set<Route> newConnections = connectedToRoute.stream().
                    map(linksPreviousDegree::get).
                    filter(found -> !found.isEmpty()).
                    collect(HashSet::new, HashSet::addAll, HashSet::addAll);

            if (!newConnections.isEmpty()) {
                final Route route = entry.getKey();
                links.put(route, newConnections);
            }
        }

        Stream<Key> keysToAdd = links.keySet().stream().
                flatMap(route -> links.get(route).stream().map(newConnection -> new Key(route, newConnection))).
                filter(key -> !costs.containsKey(key));

        int before = costs.size();
        keysToAdd.forEach(key -> costs.put(key, nextDegree));
        if (costs.size()==before) {
            links.clear();
        }

        final int added = costs.size() - before;
        logger.info("Added " + added + " extra connections for degree " + currentDegree);
        return links;
    }

    private Map<Route, Set<Route>> addInitialConnectionsFromInterchanges() {
        // seed connections between routes using interchanges
        Map<Route,Set<Route>> linksForRoutes = new HashMap<>();
        interchangeRepository.getAllInterchanges().forEach(interchange -> addOverlapsFor(interchange, linksForRoutes));
        return linksForRoutes;
    }

    private void addOverlapsFor(Station interchange, Map<Route,Set<Route>> linksForDegree) {
        List<Route> list = new ArrayList<>(interchange.getRoutes());
        int size = list.size();
        for (int i = 0; i < size; i++) {
            final Route from = list.get(i);
            if (!linksForDegree.containsKey(from)) {
                linksForDegree.put(from, new HashSet<>());
            }
            for (int j = 0; j < size; j++) {
                if (i != j) {
                    final Route towards = list.get(j);
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
        return costs.get(key);
    }

    public int size() {
        return costs.size();
    }

    public <T extends HasId<Route>> Stream<T> sortByDestinations(IdSet<Route> destinationRouteIds, Stream<T> startingRoutes) {
        Set<Route> destinations = destinationRouteIds.stream().map(routeRepository::getRouteById).collect(Collectors.toSet());
        return startingRoutes.
                map(start -> findLowestCost(destinations, start)).
                sorted(Comparator.comparingInt(Pair::getLeft)).
                map(Pair::getRight);
    }

    private <T extends HasId<Route>> Pair<Integer, T> findLowestCost(Set<Route> destinations, T hasStartId) {
        Route start = routeRepository.getRouteById(hasStartId.getId());
        if (destinations.contains(start)) {
            return Pair.of(0, hasStartId); // start on route that is present at destination
        }

        int result = destinations.stream().map(destination -> new Key(start, destination)).
                filter(costs::containsKey).
                map(key -> (int) costs.get(key)).
                min(Comparator.comparingInt(a -> a)).orElse(Integer.MAX_VALUE);
        return Pair.of(result, hasStartId);
    }


    private static class Key {

        private final IdFor<Route> first;
        private final IdFor<Route> second;

        public Key(Route routeA, Route routeB) {
            this.first = routeA.getId();
            this.second = routeB.getId();
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
                    "first=" + first +
                    ", second=" + second +
                    '}';
        }

    }
}
