package com.tramchester.graph;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Route;
import com.tramchester.domain.StationPair;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.RouteCallingStations;
import com.tramchester.repository.StationRepository;
import org.neo4j.graphdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;

import static com.tramchester.graph.TransportRelationshipTypes.ON_ROUTE;
import static java.lang.String.format;

@LazySingleton
public class RouteReachable {
    private static final Logger logger = LoggerFactory.getLogger(RouteReachable.class);

    private final GraphDatabase graphDatabaseService;
    private final StationRepository stationRepository;
    private final GraphQuery graphQuery;

    @Inject
    public RouteReachable(GraphDatabase graphDatabaseService, StationRepository stationRepository,
                          GraphQuery graphQuery,
                          StagedTransportGraphBuilder.Ready ready) {
        this.graphDatabaseService = graphDatabaseService;
        this.stationRepository = stationRepository;
        this.graphQuery = graphQuery;
    }

    // supports position inference on live data
    public List<Route> getRoutesFromStartToNeighbour(StationPair pair) {
        List<Route> results = new ArrayList<>();
        Station startStation = pair.getBegin();
        Set<Route> firstRoutes = startStation.getRoutes();
        IdFor<Station> endStationId = pair.getEnd().getId();

        try (Transaction txn = graphDatabaseService.beginTx()) {
            firstRoutes.forEach(route -> {
                RouteStation routeStation = stationRepository.getRouteStation(startStation, route);
                Node routeStationNode = graphQuery.getRouteStationNode(txn, routeStation);
                if (routeStationNode==null) {
                    logger.warn("Missing route station, graph DB rebuild needed?");
                } else {
                    Iterable<Relationship> edges = routeStationNode.getRelationships(Direction.OUTGOING, ON_ROUTE);
                    for (Relationship edge : edges) {
                        IdFor<Station> endNodeStationId = GraphProps.getStationIdFrom(edge.getEndNode());
                        if (endStationId.equals(endNodeStationId)) {
                            results.add(route);
                        }
                    }
                }
            });
        }
        return results;
    }

}
