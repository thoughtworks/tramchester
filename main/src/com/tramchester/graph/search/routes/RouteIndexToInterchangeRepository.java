package com.tramchester.graph.search.routes;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Route;
import com.tramchester.domain.places.InterchangeStation;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.repository.InterchangeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@LazySingleton
public class RouteIndexToInterchangeRepository {
    private static final Logger logger = LoggerFactory.getLogger(RouteIndexToInterchangeRepository.class);

    private final RouteIndex routeIndex;
    private final InterchangeRepository interchangeRepository;

    private final Map<RouteIndexPair, Set<InterchangeStation>> routePairToInterchange;

    @Inject
    public RouteIndexToInterchangeRepository(RouteIndex routeIndex, InterchangeRepository interchangeRepository) {
        this.routeIndex = routeIndex;
        this.interchangeRepository = interchangeRepository;
        routePairToInterchange = new HashMap<>();
    }

    @PostConstruct
    public void start() {
        logger.info("Starting");
        final Set<InterchangeStation> interchanges = interchangeRepository.getAllInterchanges();
        interchanges.forEach(this::populateFor);
        logger.info("Added interchanges for " + routePairToInterchange.size() + " route pairs");
        logger.info("Started");
    }

    @PreDestroy
    private void stop() {
        logger.info("Stop");
        routePairToInterchange.clear();
        logger.info("Stopped");
    }

    private void populateFor(InterchangeStation interchange) {

        final Set<Route> dropOffAtInterchange = interchange.getDropoffRoutes();
        final Set<Route> pickupAtInterchange = interchange.getPickupRoutes();

        for (final Route dropOff : dropOffAtInterchange) {
            final int dropOffIndex = routeIndex.indexFor(dropOff.getId());
            for (final Route pickup : pickupAtInterchange) {
                if ((!dropOff.equals(pickup)) && pickup.isDateOverlap(dropOff)) {
                    final int pickupIndex = routeIndex.indexFor(pickup.getId());
                    RouteIndexPair key = RouteIndexPair.of(dropOffIndex, pickupIndex);

                    addInterchangeBetween(key, interchange);
                }
            }
        }
    }

    private void addInterchangeBetween(RouteIndexPair pair, InterchangeStation interchange) {
        if (!routePairToInterchange.containsKey(pair)) {
            routePairToInterchange.put(pair, new HashSet<>());
        }
        routePairToInterchange.get(pair).add(interchange);
    }

    public boolean hasAnyInterchangesFor(RouteIndexPair indexPair) {
        return routePairToInterchange.containsKey(indexPair);
    }

    public Set<InterchangeStation> getInterchanges(RouteIndexPair indexPair, Set<TransportMode> requesteModes) {
        return routePairToInterchange.get(indexPair).stream().
                filter(interchangeStation -> TransportMode.intersects(requesteModes, interchangeStation.getTransportModes())).
                collect(Collectors.toSet());
    }
}
