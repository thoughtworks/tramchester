package com.tramchester.graph.search;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Route;
import com.tramchester.domain.StationPair;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.RouteCostCalculator;
import com.tramchester.metrics.TimedTransaction;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.RouteRepository;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.*;

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

        try (TimedTransaction timed = new TimedTransaction(graphDatabase, logger, "calculate inter-route costs")) {
            // note paths between routes are not symmetric
            int size = routes.size();
            Transaction txn = timed.transaction();
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    if (i != j) {
                        Route routeA = routes.get(i);
                        Route routeB = routes.get(j);
                        findCheapest(txn, routeA, routeB);
                    }
                }
            }
        }
    }

    private void findCheapest(Transaction txn, Route routeA, Route routeB) {
        Key key = new Key(routeA, routeB);
        Set<Station> starts = interchangeRepository.getInterchangesOn(routeA);
        Set<Station> ends = interchangeRepository.getInterchangesOn(routeB);
        Optional<Long> maybeCost = StationPair.combinationsOf(starts, ends).
                map(pair -> routeCostCalculator.getNumberHops(txn, pair.getBegin(), pair.getEnd())).
                min(Comparator.comparingLong(item -> item));
        maybeCost.ifPresent(cost -> costs.put(key, cost));
    }

    public long getFor(Route routeA, Route routeB) {
        Key key = new Key(routeA, routeB);
        return costs.get(key);
    }

    private static class Key {

        private final IdFor<Route> routeAId;
        private final IdFor<Route> routeBId;

        public Key(Route routeA, Route routeB) {
            this.routeAId = routeA.getId();
            this.routeBId = routeB.getId();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (!routeAId.equals(key.routeAId)) return false;
            return routeBId.equals(key.routeBId);
        }

        @Override
        public int hashCode() {
            int result = routeAId.hashCode();
            result = 31 * result + routeBId.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "Key{" +
                    "routeAId=" + routeAId +
                    ", routeBId=" + routeBId +
                    '}';
        }
    }
}
