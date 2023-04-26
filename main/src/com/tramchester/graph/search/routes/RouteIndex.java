package com.tramchester.graph.search.routes;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.caching.DataCache;
import com.tramchester.dataexport.DataSaver;
import com.tramchester.dataimport.data.RouteIndexData;
import com.tramchester.domain.Route;
import com.tramchester.domain.RoutePair;
import com.tramchester.domain.collections.RouteIndexPair;
import com.tramchester.domain.collections.RouteIndexPairFactory;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.filters.GraphFilterActive;
import com.tramchester.repository.RouteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.lang.String.format;

/***
 * Provides a map to/from an integer index for each route, which facilitates the computation of route interchanges
 * via bitmaps. Expensive to create for buses and trains, so cacheable.
 */
@LazySingleton
public class RouteIndex implements DataCache.CachesData<RouteIndexData> {
    private static final Logger logger = LoggerFactory.getLogger(RouteIndex.class);

    private final RouteRepository routeRepository;
    private final GraphFilterActive graphFilter;
    private final DataCache dataCache;
    private final RouteIndexPairFactory pairFactory;

    private final Map<Route, Integer> mapRouteIdToIndex;
    private final Map<Integer, Route> mapIndexToRouteId;
    private final int numberOfRoutes;

    @Inject
    public RouteIndex(RouteRepository routeRepository, GraphFilterActive graphFilter, DataCache dataCache, RouteIndexPairFactory pairFactory) {
        this.routeRepository = routeRepository;
        this.numberOfRoutes = routeRepository.numberOfRoutes();
        this.graphFilter = graphFilter;
        this.dataCache = dataCache;
        this.pairFactory = pairFactory;

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

        // here for past serialisation issues
        if (mapRouteIdToIndex.size()!=mapIndexToRouteId.size()) {
            String msg = format("Constraints on mapping violated mapRouteIdToIndex %s != mapIndexToRouteId %s"
                    , mapRouteIdToIndex.size(), mapIndexToRouteId.size());
            logger.error(msg);
            throw new RuntimeException(msg);
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
        List<Route> routesList = new ArrayList<>(routeRepository.getRoutes());
        createIndex(routesList);
        logger.info("Added " + mapRouteIdToIndex.size() + " index entries");
    }

    private void createIndex(List<Route> routesList) {
        for (int i = 0; i < routesList.size(); i++) {
            mapRouteIdToIndex.put(routesList.get(i), i);
            mapIndexToRouteId.put(i, routesList.get(i));
        }
    }

    public int indexFor(IdFor<Route> routeId) {
        Route route = routeRepository.getRouteById(routeId);
        return indexFor(route);
    }

    private Integer indexFor(Route route) {
        if (!(mapRouteIdToIndex.containsKey(route))) {
            String message = format("No index for route %s, is cache file %s outdated? ",
                    route.getId(), dataCache.getPathFor(this));
            logger.error(message);
            throw new RuntimeException(message);
        }
        return mapRouteIdToIndex.get(route);
    }

    @Override
    public void cacheTo(DataSaver<RouteIndexData> saver) {
        saver.open();
        mapRouteIdToIndex.entrySet().stream().
                map(entry -> new RouteIndexData(entry.getValue(), entry.getKey().getId())).
                forEach(saver::write);
        saver.close();
    }

    @Override
    public String getFilename() {
        return RouteToRouteCosts.INDEX_FILE;
    }

    @Override
    public void loadFrom(Stream<RouteIndexData> stream) throws DataCache.CacheLoadException {
        logger.info("Loading from cache");
        IdSet<Route> missingRouteIds = new IdSet<>();
        stream.forEach(item -> {
            final IdFor<Route> routeId = item.getRouteId();
            if (!routeRepository.hasRouteId(routeId)) {
                String message = "RouteId not found in repository: " + routeId;
                logger.error(message);
                missingRouteIds.add(routeId);
                //throw new RuntimeException(message);
            }
            Route route = routeRepository.getRouteById(routeId);
            mapRouteIdToIndex.put(route, item.getIndex());
            mapIndexToRouteId.put(item.getIndex(), route);
        });
        if (!missingRouteIds.isEmpty()) {
            String msg = format("The following routeIds present in index file but not the route repository (size %s) %s",
                    routeRepository.numberOfRoutes(), missingRouteIds);
            // TODO debug?
            logger.warn("Routes in repo: " + HasId.asIds(routeRepository.getRoutes()));
            throw new DataCache.CacheLoadException(msg);
        }
        if (mapRouteIdToIndex.size() != numberOfRoutes) {
            String msg = "Mismatch on number of routes, from index got: " + mapRouteIdToIndex.size() +
                    " but repository has: " + numberOfRoutes;
            logger.error(msg);
            throw new DataCache.CacheLoadException(msg);
        }
    }

    public Route getRouteFor(int index) {
        return mapIndexToRouteId.get(index);
    }

    public RoutePair getPairFor(RouteIndexPair indexPair) {
        Route first = mapIndexToRouteId.get(indexPair.first());
        if (first==null) {
            throw new RuntimeException("Could not find first Route for index " + indexPair);
        }
        Route second = mapIndexToRouteId.get(indexPair.second());
        if (second==null) {
            throw new RuntimeException("Could not find second Route for index " + indexPair);
        }

        return new RoutePair(first, second);
    }

    public RouteIndexPair getPairFor(RoutePair routePair) {
        int a = indexFor(routePair.first().getId());
        int b = indexFor(routePair.second().getId());
        return pairFactory.get(a, b);
    }

    public boolean hasIndexFor(IdFor<Route> routeId) {
        Route route = routeRepository.getRouteById(routeId);
        return mapRouteIdToIndex.containsKey(route);
    }

    public long sizeFor(TransportMode mode) {
        return mapRouteIdToIndex.keySet().stream().filter(route -> route.getTransportMode().equals(mode)).count();
    }
}
