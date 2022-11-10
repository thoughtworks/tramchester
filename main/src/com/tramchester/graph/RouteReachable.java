package com.tramchester.graph;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Route;
import com.tramchester.domain.StationPair;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.repository.StationAvailabilityRepository;
import com.tramchester.repository.StationRepository;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.tramchester.graph.TransportRelationshipTypes.ON_ROUTE;

@LazySingleton
public class RouteReachable {
    private static final Logger logger = LoggerFactory.getLogger(RouteReachable.class);

    private final GraphDatabase graphDatabaseService;
    private final StationRepository stationRepository;
    private final GraphQuery graphQuery;
    private final StationAvailabilityRepository availabilityRepository;

    @Inject
    public RouteReachable(GraphDatabase graphDatabaseService, StationRepository stationRepository,
                          GraphQuery graphQuery,
                          StagedTransportGraphBuilder.Ready ready,
                          StationAvailabilityRepository availabilityRepository) {
        this.graphDatabaseService = graphDatabaseService;
        this.stationRepository = stationRepository;
        this.graphQuery = graphQuery;
        this.availabilityRepository = availabilityRepository;
    }

    // supports position inference on live data
    public List<Route> getRoutesFromStartToNeighbour(StationPair pair, TramDate date, TimeRange timeRange) {
        List<Route> results = new ArrayList<>();
        Station startStation = pair.getBegin();
        //Set<Route> firstRoutes = startStation.getPickupRoutes(); // pickups from first station
        Set<Route> firstRoutes = availabilityRepository.getPickupRoutesFor(startStation, date, timeRange);
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
