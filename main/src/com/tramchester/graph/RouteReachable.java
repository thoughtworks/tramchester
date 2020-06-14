package com.tramchester.graph;

import com.tramchester.domain.Route;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.repository.InterchangeRepository;
import org.neo4j.graphalgo.*;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.tramchester.graph.GraphStaticKeys.*;
import static com.tramchester.graph.GraphBuilder.Labels.ROUTE_STATION;
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
    public boolean getRouteReachableWithInterchange(Route route, String startStationId, String endStationId) {
        return hasAnyPaths(route, startStationId, endStationId);
    }

    // supports position inference on live data
    public List<Route> getRoutesFromStartToNeighbour(Station startStation, String endStationId) {
        List<Route> results = new ArrayList<>();
        Set<Route> firstRoutes = startStation.getRoutes();

        try (Transaction txn = graphDatabaseService.beginTx()) {
            firstRoutes.forEach(route -> {
                Node routeStation = graphQuery.getRouteStationNode(txn, RouteStation.formId(startStation, route));
                Iterable<Relationship> edges = routeStation.getRelationships(Direction.OUTGOING, ON_ROUTE);
                for (Relationship edge : edges) {
                    if (endStationId.equals(edge.getEndNode().getProperty(STATION_ID).toString())) {
                        results.add(route);
                    }
                }
            });
        }
        return results;
    }

    // TODO WIP to support bus routing
    public Map<String,String> getShortestRoutesBetween(Station startStation, Station endStation) {
        HashMap<String, String> results = new HashMap<>();
        try (Transaction txn = graphDatabaseService.beginTx()) {
            Node startNode = graphQuery.getStationNode(txn, startStation);
            Node endNode = graphQuery.getStationNode(txn, endStation);

            EvaluationContext context = graphDatabaseService.createContext(txn);
            PathFinder<WeightedPath> finder = GraphAlgoFactory.dijkstra(context, fullExpanderForRoutes(), COST);
            Iterable<WeightedPath> paths = finder.findAllPaths(startNode, endNode);

            paths.forEach(path -> path.relationships().forEach(relationship -> {

                if (relationship.isType(BOARD) || relationship.isType(INTERCHANGE_BOARD)) {
                    Node node = relationship.getEndNode();
                    String routeId = node.getProperty(ROUTE_ID).toString();
                    String stationId = node.getProperty(STATION_ID).toString();
                    results.put(stationId, routeId);
                }
            }));
            txn.commit();
        }

        return results;
    }

    private boolean hasAnyPaths(Route route, String startStationId, String endStationId) {

        boolean hasAny = false;
        try (Transaction txn = graphDatabaseService.beginTx()) {

            Node found = graphQuery.getTramStationNode(txn, startStationId);
            if (found==null) {
                if (warnForMissing) {
                    logger.warn("Cannot find node for station id " + startStationId);
                }
            } else  {
                Evaluator evaluator = new FindRouteNodesForDesintationAndRouteId<>(endStationId, route.getId(),
                        interchangeRepository);

                TraversalDescription traverserDesc = txn.traversalDescription().
                        order(BranchOrderingPolicies.PREORDER_DEPTH_FIRST).
                        relationships(ON_ROUTE, Direction.OUTGOING).
                        relationships(ENTER_PLATFORM, Direction.OUTGOING).
                        relationships(BOARD, Direction.OUTGOING).
                        relationships(INTERCHANGE_BOARD, Direction.OUTGOING).
                        evaluator(evaluator);

                Traverser traverser = traverserDesc.traverse(found);

                for(Path path : traverser) {
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
        PathFinder<WeightedPath> finder = GraphAlgoFactory.dijkstra(context, forTypesAndDirections, COST);
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
        private final String endStationId;
        private final String routeId;
        private final InterchangeRepository interchangeRepository;

        public FindRouteNodesForDesintationAndRouteId(String endStationId, String routeId,
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
            Node queryNode = path.endNode();

            if (queryNode.hasLabel(ROUTE_STATION)) {
                String currentStationId = queryNode.getProperty(STATION_ID).toString();
                if (endStationId.equals(currentStationId)) {
                    return Evaluation.INCLUDE_AND_PRUNE; // finished, at dest
                }
                if (interchangeRepository.isInterchange(currentStationId)) {
                    return Evaluation.INCLUDE_AND_PRUNE; // finished, at interchange
                }
            }

            if (queryNode.hasRelationship(Direction.OUTGOING, ON_ROUTE)) {
                if (routeId.isEmpty()) {
                    return Evaluation.EXCLUDE_AND_CONTINUE;
                }
                Iterable<Relationship> routeRelat = queryNode.getRelationships(Direction.OUTGOING, ON_ROUTE);
                Set<String> routeIds = new HashSet<>();
                routeRelat.forEach(relationship -> routeIds.add(relationship.getProperty(ROUTE_ID).toString()));

                //String id = routeRelat.getProperty(ROUTE_ID).toString();
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
