package com.tramchester.graph;

import com.tramchester.domain.IdFor;
import com.tramchester.domain.IdSet;
import com.tramchester.domain.Route;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.graphbuild.GraphFilter;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.StationRepository;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static com.tramchester.graph.graphbuild.GraphBuilder.Labels.ROUTE_STATION;

public class RouteReachable {
    private static final Logger logger = LoggerFactory.getLogger(RouteReachable.class);

    private final GraphDatabase graphDatabaseService;
    private final InterchangeRepository interchangeRepository;
    private final StationRepository stationRepository;
    private final boolean warnForMissing;
    private final GraphQuery graphQuery;

    public RouteReachable(GraphDatabase graphDatabaseService,
                          InterchangeRepository interchangeRepository, StationRepository stationRepository, GraphFilter filter,
                          GraphQuery graphQuery) {
        this.graphDatabaseService = graphDatabaseService;
        this.interchangeRepository = interchangeRepository;
        this.stationRepository = stationRepository;
        this.warnForMissing = !filter.isFiltered();
        this.graphQuery = graphQuery;
    }

    // supports building tram station reachability matrix
    public boolean getRouteReachableWithInterchange(RouteStation routeStation, Station endStation) {
        return hasAnyPaths(routeStation, endStation);
    }

    // supports position inference on live data
    public List<Route> getRoutesFromStartToNeighbour(Station startStation, Station endStation) {
        List<Route> results = new ArrayList<>();
        Set<Route> firstRoutes = startStation.getRoutes();
        IdFor<Station> endStationId = endStation.getId();

        try (Transaction txn = graphDatabaseService.beginTx()) {
            firstRoutes.forEach(route -> {
                RouteStation routeStation = stationRepository.getRouteStation(startStation, route);
                Node routeStationNode = graphQuery.getRouteStationNode(txn, routeStation);
                Iterable<Relationship> edges = routeStationNode.getRelationships(Direction.OUTGOING, ON_ROUTE);
                for (Relationship edge : edges) {
                    if (endStationId.matchesStationNodePropery(edge.getEndNode())) {
                        results.add(route);
                    }
                }
            });
        }
        return results;
    }

    private boolean hasAnyPaths(RouteStation routeStation, Station endStation) {

        boolean hasAny = false;
        try (Transaction txn = graphDatabaseService.beginTx()) {

            Node found = graphQuery.getStationNode(txn, routeStation.getStation());
            if (found==null) {
                if (warnForMissing) {
                    logger.warn("Cannot find node for station id " + routeStation);
                }
            } else  {
                IdFor<Station> endStationId = endStation.getId();
                Evaluator evaluator = new FindRouteNodesForDesintationAndRouteId<>(endStationId, routeStation.getRoute().getId(),
                        interchangeRepository);

                TraversalDescription traverserDesc = graphDatabaseService.traversalDescription(txn).
                        order(BranchOrderingPolicies.PREORDER_DEPTH_FIRST).
                        relationships(ON_ROUTE, Direction.OUTGOING).
                        relationships(ENTER_PLATFORM, Direction.OUTGOING).
                        relationships(BOARD, Direction.OUTGOING).
                        relationships(INTERCHANGE_BOARD, Direction.OUTGOING).
                        evaluator(evaluator);

                Traverser traverser = traverserDesc.traverse(found);

                for(Path ignored : traverser) {
                    hasAny = true;
                }
            }
        }
        return hasAny;
    }

    private static class FindRouteNodesForDesintationAndRouteId<STATE> implements PathEvaluator<STATE> {
        private final IdFor<Station> endStationId;
        private final IdFor<Route> routeId;
        private final InterchangeRepository interchangeRepository;

        public FindRouteNodesForDesintationAndRouteId(IdFor<Station> endStationId, IdFor<Route> routeId,
                                                      InterchangeRepository interchangeRepository) {
            this.endStationId = endStationId;
            this.routeId = routeId;
            this.interchangeRepository = interchangeRepository;
        }

        @Override
        public Evaluation evaluate( Path path ) {
            return evaluate(path, BranchState.NO_STATE);
        }

        @Override
        public Evaluation evaluate(Path path, BranchState state) {
            Node endNode = path.endNode();

            if (endNode.hasLabel(ROUTE_STATION)) {
                IdFor<Station> currentStationId = IdFor.getStationIdFrom(endNode);
                if (endStationId.equals(currentStationId)) {
                    return Evaluation.INCLUDE_AND_PRUNE; // finished, at dest
                }
                if (interchangeRepository.isInterchange(currentStationId)) {
                    return Evaluation.INCLUDE_AND_PRUNE; // finished, at interchange
                }
            }

            if (endNode.hasRelationship(Direction.OUTGOING, ON_ROUTE)) {
                // TODO why was this needed?
//                if (routeId.isEmpty()) {
//                    return Evaluation.EXCLUDE_AND_CONTINUE;
//                }
                Iterable<Relationship> routeRelat = endNode.getRelationships(Direction.OUTGOING, ON_ROUTE);
                IdSet<Route> routeIds = new IdSet<>();
                routeRelat.forEach(relationship -> routeIds.add(IdFor.getRouteIdFrom(relationship)));

                if (routeIds.contains(routeId)) {
                    return Evaluation.EXCLUDE_AND_CONTINUE; // if have routeId then only follow if on same route
                } else {
                    return Evaluation.EXCLUDE_AND_PRUNE;
                }
            }

            return Evaluation.EXCLUDE_AND_CONTINUE;
        }
    }

}
