package com.tramchester.repository;

import com.tramchester.graph.TramRouteReachable;
import com.tramchester.graph.TransportGraphBuilder;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.tramchester.graph.GraphStaticKeys.*;
import static java.lang.String.format;

public class ReachabilityRepository {
    private static final Logger logger = LoggerFactory.getLogger(RoutesRepository.class);

    private final GraphDatabaseService graphDatabaseService;
    private final TramRouteReachable tramRouteReachable;

    private List<String> stationIds; // a list as we need ordering and IndexOf
    private Map<String, boolean[]> matrix; // stationId -> boolean[]

    public ReachabilityRepository(GraphDatabaseService graphDatabaseService, TramRouteReachable tramRouteReachable) {
        this.graphDatabaseService = graphDatabaseService;
        this.tramRouteReachable = tramRouteReachable;
        stationIds = new ArrayList<>();
        matrix = new HashMap<>();
    }

    public void buildRepository() {
        logger.info("Build repository");
        Map<String, RouteStationEntry> routeStations = new HashMap<>(); // routestationId -> entry
        try (Transaction tx = graphDatabaseService.beginTx()) {
            ResourceIterator<Node> nodes = graphDatabaseService.findNodes(TransportGraphBuilder.Labels.ROUTE_STATION);
            nodes.stream().forEach(node -> {
                Map<String, Object> props = node.getProperties(STATION_ID, ROUTE_ID, ID);

                String routeStationId = props.get(ID).toString();
                String stationId = props.get(STATION_ID).toString();
                RouteStationEntry routeStation = new RouteStationEntry(stationId, props.get(ROUTE_ID).toString());
                routeStations.put(routeStationId,routeStation);
                if (!stationIds.contains(stationId)) {
                    stationIds.add(stationId);
                }
            });
        }

        int size = stationIds.size();
        routeStations.forEach((routeStationId,entry) -> {
            boolean[] flags = new boolean[size];
            stationIds.forEach(destinationStationId -> {
                if (destinationStationId==entry.stationId) {
                    flags[stationIds.indexOf(destinationStationId)] = true;
                } else {
                    boolean flag = tramRouteReachable.getRouteReachableWithInterchange(
                            entry.stationId, destinationStationId, entry.routeId);
                    flags[stationIds.indexOf(destinationStationId)] = flag;
                }
            });
            matrix.put(routeStationId, flags);
        });
        logger.info(format("Added %s entries", size));
    }

    public boolean reachable(String routeStationId, String destinationStationId) {
        int index = stationIds.indexOf(destinationStationId);
        return matrix.get(routeStationId)[index];
    }

    private class RouteStationEntry {
        private final String stationId;
        private final String routeId;

        public RouteStationEntry(String stationId, String routeId) {
            this.stationId = stationId;
            this.routeId = routeId;
        }
    }
}
