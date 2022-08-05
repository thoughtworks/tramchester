package com.tramchester.graph.search.routes;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.caching.DataCache;
import com.tramchester.dataexport.DataSaver;
import com.tramchester.dataimport.data.RouteIndexData;
import com.tramchester.domain.Route;
import com.tramchester.domain.RoutePair;
import com.tramchester.domain.id.IdFor;
import com.tramchester.graph.filters.GraphFilterActive;
import com.tramchester.repository.RouteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/***
 * Provides a map to/from an integer index for each route, which facilitates the computation of route interchanges
 * via bitmaps. Expensive to create for buses and trains, so cacheable.
 */
@LazySingleton
class RouteIndex implements DataCache.Cacheable<RouteIndexData> {
    private static final Logger logger = LoggerFactory.getLogger(RouteIndex.class);

    private final RouteRepository routeRepository;
    private final GraphFilterActive graphFilter;
    private final DataCache dataCache;

    private final Map<IdFor<Route>, Integer> mapRouteIdToIndex;
    private final Map<Integer, IdFor<Route>> mapIndexToRouteId;
    private final int numberOfRoutes;

    @Inject
    public RouteIndex(RouteRepository routeRepository, GraphFilterActive graphFilter, DataCache dataCache) {
        this.routeRepository = routeRepository;
        this.numberOfRoutes = routeRepository.numberOfRoutes();
        this.graphFilter = graphFilter;
        this.dataCache = dataCache;

        mapRouteIdToIndex = new HashMap<>(numberOfRoutes);
        mapIndexToRouteId = new HashMap<>(numberOfRoutes);
    }

    @PostConstruct
    public void start() {
        logger.info("starting");
        if (graphFilter.isActive()) {
            logger.warn("Filtering is enabled, skipping all caching");
            createIndex();
        } else {
            if (dataCache.has(this)) {
                logger.info("Loading from cache");
                dataCache.loadInto(this, RouteIndexData.class);
            } else {
                logger.info("Not in cache, creating");
                createIndex();
                dataCache.save(this, RouteIndexData.class);
            }
        }
        logger.info("started");
    }

    @PreDestroy
    public void clear() {
        logger.info("Stopping");
        mapRouteIdToIndex.clear();
        mapIndexToRouteId.clear();
        logger.info("Stopped");
    }

    private void createIndex() {
        logger.info("Creating index");
        List<IdFor<Route>> routesList = routeRepository.getRoutes().stream().map(Route::getId).collect(Collectors.toList());
        createIndex(routesList);
        logger.info("Added " + mapRouteIdToIndex.size() + " index entries");
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
        return RouteToRouteCosts.INDEX_FILE;
    }

    @Override
    public void loadFrom(Stream<RouteIndexData> stream) throws DataCache.CacheLoadException {
        logger.info("Loading from cache");
        stream.forEach(item -> {
            mapRouteIdToIndex.put(item.getRouteId(), item.getIndex());
            mapIndexToRouteId.put(item.getIndex(), item.getRouteId());
        });
        if (mapRouteIdToIndex.size() != numberOfRoutes) {
            throw new DataCache.CacheLoadException("Mismatch on number of routes, got " + mapRouteIdToIndex.size() +
                    " expected " + numberOfRoutes);
        }
    }

    public Route getRouteFor(int index) {
        return routeRepository.getRouteById(mapIndexToRouteId.get(index));
    }

    public RoutePair getPairFor(RouteIndexPair indexPair) {
        IdFor<Route> firstId = mapIndexToRouteId.get(indexPair.first());
        IdFor<Route> secondId = mapIndexToRouteId.get(indexPair.second());

        Route first = routeRepository.getRouteById(firstId);
        Route second = routeRepository.getRouteById(secondId);

        return new RoutePair(first, second);
    }

    public RouteIndexPair getPairFor(RoutePair routePair) {
        int a = indexFor(routePair.getFirst().getId());
        int b = indexFor(routePair.getSecond().getId());
        return RouteIndexPair.of(a, b);
    }


}
