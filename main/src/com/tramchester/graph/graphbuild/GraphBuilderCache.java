package com.tramchester.graph.graphbuild;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.Platform;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import org.apache.commons.lang3.tuple.Pair;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.lang.String.format;

@LazySingleton
public class GraphBuilderCache {
    private static final Logger logger = LoggerFactory.getLogger(GraphBuilderCache.class);

    private boolean cleared;
    private final Map<IdFor<RouteStation>, Long> routeStations;
    private final Map<Station, Long> stations;
    private final Map<IdFor<Platform>, Long> platforms;
    private final Map<String, Long> svcNodes;
    private final Map<String, Long> hourNodes;
    private final Set<Pair<Long, Long>> boardings;
    private final Set<Pair<Long, Long>> departs;

    @Inject
    public GraphBuilderCache() {
        cleared = false;
        stations = new HashMap<>();
        routeStations = new HashMap<>();
        platforms = new HashMap<>();
        svcNodes = new HashMap<>();
        hourNodes = new HashMap<>();
        boardings = new HashSet<>();
        departs = new HashSet<>();
    }

    protected void fullClear() {
        if (cleared) {
            throw new RuntimeException("Already cleared");
        }
        routeStations.clear();
        stations.clear();
        platforms.clear();
        svcNodes.clear();
        hourNodes.clear();
        cleared = true;
        logger.info("Full cleared");
    }

    // memory usage management
    protected void routeClear() {
        platforms.clear();
        svcNodes.clear();
        hourNodes.clear();
        logger.debug("Route Clear");
    }

    public void putRouteStation(IdFor<RouteStation> id, Node routeStationNode) {
        routeStations.put(id, routeStationNode.getId());
    }

    protected void putStation(Station station, Node stationNode) {
        stations.put(station, stationNode.getId());
    }

    protected Node getRouteStation(Transaction txn, Route route, Station station) {
        IdFor<RouteStation> id = IdFor.createId(station, route);
        if (!routeStations.containsKey(id)) {
            String message = "Cannot find routestation node in cache " + id + " station "
                    + station.getId() + " route " + route.getId();
            logger.error(message);
            throw new RuntimeException(message);
        }
        return txn.getNodeById(routeStations.get(id));
    }

    protected Node getStation(Transaction txn, Station station) {
        return txn.getNodeById(stations.get(station));
    }

    protected Node getPlatform(Transaction txn, IdFor<Platform> platformId) {
        return txn.getNodeById(platforms.get(platformId));
    }

    protected void putPlatform(IdFor<Platform> platformId, Node platformNode) {
        platforms.put(platformId, platformNode.getId());
    }

    protected Node getServiceNode(Transaction txn, Service service, Station startStation, Station endStation) {
        String id = CreateKeys.getServiceKey(service, startStation, endStation);
        return txn.getNodeById(svcNodes.get(id));
    }

    protected void putService(Service service, Station begin, Station end, Node svcNode) {
        svcNodes.put(CreateKeys.getServiceKey(service, begin, end), svcNode.getId());
    }

    protected void putHour(Service service, Station station, Integer hour, Node node) {
        hourNodes.put(CreateKeys.getHourKey(service, station, hour), node.getId());
    }

    protected Node getHourNode(Transaction txn, Service service, Station station, Integer hour) {
        String key = CreateKeys.getHourKey(service, station, hour);
        if (!hourNodes.containsKey(key)) {
            throw new RuntimeException(format("Missing hour node for key %s service %s station %s hour %s",
                    key, service.getId(), station.getId(), hour.toString()));
        }
        return txn.getNodeById(hourNodes.get(key));
    }

    protected boolean hasBoarding(long boardingNodeId, long routeStationNodeId) {
        return boardings.contains(Pair.of(boardingNodeId, routeStationNodeId));
    }

    protected void putBoarding(long boardingNodeId, long routeStationNodeId) {
        boardings.add(Pair.of(boardingNodeId, routeStationNodeId));
    }

    protected boolean hasDeparts(long routeStationNodeId, long boardingNodeId) {
        return departs.contains(Pair.of(routeStationNodeId, boardingNodeId));
    }

    protected void putDepart(long boardingNodeId, long routeStationNodeId) {
        departs.add(Pair.of(routeStationNodeId, boardingNodeId));
    }

    private static class CreateKeys {
        protected static String getServiceKey(Service service, Station startStation, Station endStation) {
            return startStation.getId().getGraphId()+"_"+endStation.getId().getGraphId()+"_"+ service.getId().getGraphId();
        }

        protected static String getHourKey(Service service, Station station, Integer hour) {
            return service.getId().getGraphId()+"_"+station.getId().getGraphId()+"_"+hour.toString();
        }

    }
}
