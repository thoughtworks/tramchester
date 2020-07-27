package com.tramchester.graph;

import com.tramchester.domain.IdFor;
import com.tramchester.domain.IdSet;
import com.tramchester.domain.Route;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.graphbuild.GraphFilter;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.repository.InterchangeRepository;
import org.neo4j.graphalgo.*;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.tramchester.graph.GraphPropertyKeys.*;
import static com.tramchester.graph.graphbuild.GraphBuilder.Labels.ROUTE_STATION;
import static com.tramchester.graph.TransportRelationshipTypes.*;

public class RouteReachable {
    private static final Logger logger = LoggerFactory.getLogger(RouteReachable.class);

    private final GraphDatabase graphDatabaseService;
    private final InterchangeRepository interchangeRepository;
    private final boolean warnForMissing;
    private final GraphQuery graphQuery;

    public RouteReachable(GraphDatabase graphDatabaseService,
                          InterchangeRepository interchangeRepository, GraphFilter filter,
                          GraphQuery graphQuery) {
        this.graphDatabaseService = graphDatabaseService;
        this.interchangeRepository = interchangeRepository;
        this.warnForMissing = !filter.isFiltered();
        this.graphQuery = graphQuery;
    }

    // supports building tram station reachability matrix
    public boolean getRouteReachableWithInterchange(Route route, Station startStation, Station endStation) {
        return hasAnyPaths(route, startStation, endStation);
    }

    // supports position inference on live data
    public List<Route> getRoutesFromStartToNeighbour(Station startStation, Station endStation) {
        List<Route> results = new ArrayList<>();
        Set<Route> firstRoutes = startStation.getRoutes();
        IdFor<Station> endStationId = endStation.getId();

        try (Transaction txn = graphDatabaseService.beginTx()) {
            firstRoutes.forEach(route -> {
                Node routeStation = graphQuery.getRouteStationNode(txn, IdFor.createId(startStation, route));
                Iterable<Relationship> edges = routeStation.getRelationships(Direction.OUTGOING, ON_ROUTE);
                for (Relationship edge : edges) {
//                    if (endStationId.equals(edge.getEndNode().getProperty(STATION_ID).toString())) {
                    if (endStationId.matchesStationNodePropery(edge.getEndNode())) {
                        results.add(route);
                    }
                }
            });
        }
        return results;
    }

    // TEST ONLY
    // TODO WIP to support bus routing
    public Map<IdFor<Station>, IdFor<Route>> getShortestRoutesBetween(Station startStation, Station endStation) {
        HashMap<IdFor<Station>, IdFor<Route>> results = new HashMap<>();
        try (Transaction txn = graphDatabaseService.beginTx()) {
            Node startNode = graphQuery.getStationNode(txn, startStation);
            Node endNode = graphQuery.getStationNode(txn, endStation);

            EvaluationContext context = graphDatabaseService.createContext(txn);
            PathFinder<WeightedPath> finder = GraphAlgoFactory.dijkstra(context, fullExpanderForRoutes(), COST.getText());
            Iterable<WeightedPath> paths = finder.findAllPaths(startNode, endNode);

            paths.forEach(path -> path.relationships().forEach(relationship -> {

                if (relationship.isType(BOARD) || relationship.isType(INTERCHANGE_BOARD)) {
                    Node node = relationship.getEndNode();
                    IdFor<Route> routeId = IdFor.getRouteIdFrom(node); //node.getProperty(ROUTE_ID).toString();
                    IdFor<Station> stationId = GraphProps.getStationId(node);
                    results.put(stationId, routeId);
                }
            }));
            txn.commit();
        }

        return results;
    }

    private boolean hasAnyPaths(Route route, Station startStation, Station endStation) {

        boolean hasAny = false;
        try (Transaction txn = graphDatabaseService.beginTx()) {

            Node found = graphQuery.getStationNode(txn, startStation);
            if (found==null) {
                if (warnForMissing) {
                    logger.warn("Cannot find node for station id " + startStation);
                }
            } else  {
                IdFor<Station> endStationId = endStation.getId();
                Evaluator evaluator = new FindRouteNodesForDesintationAndRouteId<>(endStationId, route.getId(),
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

    public int getApproxCostBetween(Transaction txn, Station station, Node endNode) {
        Node startNode = graphQuery.getStationNode(txn, station);
        return getApproxCostBetween(txn, startNode, endNode);
    }

    // startNode must have been found within supplied txn
    public int getApproxCostBetween(Transaction txn, Node startNode, Station endStation) {
        Node endNode = graphQuery.getStationNode(txn, endStation);
        return getApproxCostBetween(txn, startNode, endNode);
    }

    public int getApproxCostBetween(Transaction txn, Station startStation, Station endStation) {
        Node startNode = graphQuery.getStationNode(txn, startStation);
        Node endNode = graphQuery.getStationNode(txn, endStation);
        return getApproxCostBetween(txn, startNode, endNode);
    }

    // startNode and endNode must have been found within supplied txn
    public int getApproxCostBetween(Transaction txn, Node startNode, Node endNode) {
        // follow the ON_ROUTE relationships to quickly find a route without any timing information or check
        PathExpander<Double> forTypesAndDirections = fullExpanderForRoutes();

        EvaluationContext context = graphDatabaseService.createContext(txn);
        PathFinder<WeightedPath> finder = GraphAlgoFactory.dijkstra(context, forTypesAndDirections, COST.getText());
        WeightedPath path = finder.findSinglePath(startNode, endNode);
        double weight  = Math.floor(path.weight());
        return (int) weight;
    }

    private PathExpander<Double> fullExpanderForRoutes() {
        return PathExpanders.forTypesAndDirections(
                ON_ROUTE, Direction.OUTGOING,
                ENTER_PLATFORM, Direction.OUTGOING,
                LEAVE_PLATFORM, Direction.OUTGOING,
                BOARD, Direction.OUTGOING,
                DEPART, Direction.OUTGOING,
                INTERCHANGE_BOARD, Direction.OUTGOING,
                INTERCHANGE_DEPART, Direction.OUTGOING,
                WALKS_TO, Direction.OUTGOING,
                WALKS_FROM, Direction.OUTGOING,
                FINISH_WALK, Direction.OUTGOING
        );
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
