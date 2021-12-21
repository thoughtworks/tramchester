package com.tramchester.graph;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.RouteRepository;
import org.assertj.core.util.Streams;
import org.neo4j.graphalgo.EvaluationContext;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.function.Predicate;

import static com.tramchester.graph.GraphPropertyKey.COST;
import static com.tramchester.graph.GraphPropertyKey.MAX_COST;
import static com.tramchester.graph.TransportRelationshipTypes.*;
import static java.lang.String.format;

/***
 * Supports arrive-by calculations by finding an approx cost for a specific journey
 */
@LazySingleton
public class RouteCostCalculator {
    private static final Logger logger = LoggerFactory.getLogger(RouteCostCalculator.class);

    private final GraphQuery graphQuery;
    private final GraphDatabase graphDatabaseService;
    private final InterchangeRepository interchangeRepository;
    private final RouteRepository routeRepository;

    @Inject
    public RouteCostCalculator(GraphQuery graphQuery, GraphDatabase graphDatabaseService,
                               StagedTransportGraphBuilder.Ready ready, InterchangeRepository interchangeRepository, RouteRepository routeRepository) {
        this.graphQuery = graphQuery;
        this.graphDatabaseService = graphDatabaseService;
        this.interchangeRepository = interchangeRepository;
        this.routeRepository = routeRepository;
    }

    public int getAverageCostBetween(Transaction txn, Node startNode, Node endNode, TramServiceDate date) {
        return calculateLeastCost(txn, startNode, endNode, COST, date.getDate());
    }

    public int getAverageCostBetween(Transaction txn, Station station, Node endNode, TramServiceDate date) {
        Node startNode = graphQuery.getStationOrGrouped(txn, station);
        return calculateLeastCost(txn, startNode, endNode, COST, date.getDate());
    }

    // startNode must have been found within supplied txn
    public int getAverageCostBetween(Transaction txn, Node startNode, Station endStation, TramServiceDate date) {
        Node endNode = graphQuery.getStationOrGrouped(txn, endStation);
        return calculateLeastCost(txn, startNode, endNode, COST, date.getDate());
    }

    public int getAverageCostBetween(Transaction txn, Station startStation, Station endStation, TramServiceDate date) {
        return getCostBetween(txn, startStation, endStation, COST, date.getDate());
    }

    public int getMaxCostBetween(Transaction txn, Station start, Station end, TramServiceDate date) {
        return getCostBetween(txn, start, end, MAX_COST, date.getDate());
    }

    private int getCostBetween(Transaction txn, Station startStation, Station endStation, GraphPropertyKey key, LocalDate date) {
        Node startNode = graphQuery.getStationOrGrouped(txn, startStation);
        if (startNode==null) {
            throw new RuntimeException("Could not find start node for graph id " + startStation.getId().getGraphId());
        }
        Node endNode = graphQuery.getStationOrGrouped(txn, endStation);
        if (endNode==null) {
            throw new RuntimeException("Could not find end node for graph id" + endStation.getId().getGraphId());
        }
        logger.info(format("Find approx. route cost between %s and %s", startStation.getId(), endStation.getId()));

        return calculateLeastCost(txn, startNode, endNode, key, date);
    }

    // startNode and endNode must have been found within supplied txn

    private int calculateLeastCost(Transaction txn, Node startNode, Node endNode, GraphPropertyKey key, LocalDate date) {
        IdSet<Route> running = routeRepository.getRoutesRunningOn(date);
        // TODO fetch all the relationshipIds first

        EvaluationContext context = graphDatabaseService.createContext(txn);

        Predicate<? super Relationship> routeFilter = (Predicate<Relationship>) relationship ->
                !relationship.isType(ON_ROUTE) || running.contains(GraphProps.getRouteIdFrom(relationship));

        PathExpander<Double> forTypesAndDirections = fullExpanderForCostApproximation(routeFilter);

        PathFinder<WeightedPath> finder = GraphAlgoFactory.dijkstra(context, forTypesAndDirections, key.getText());

        WeightedPath path = finder.findSinglePath(startNode, endNode);
        if (path==null) {
            logger.error(format("No path found between node %s and node %s", startNode.getId(), endNode.getId()));
            return -1;
        }
        double weight  = Math.floor(path.weight());
        return (int) weight;
    }

    private PathExpander<Double> fullExpanderForCostApproximation(Predicate<? super Relationship> routeFilter) {
        return PathExpanderBuilder.empty().
                add(ON_ROUTE, Direction.OUTGOING).
                add(STATION_TO_ROUTE, Direction.OUTGOING).
                add(ROUTE_TO_STATION, Direction.OUTGOING).
                add(WALKS_TO, Direction.OUTGOING).
                add(WALKS_FROM, Direction.OUTGOING).
                add(NEIGHBOUR, Direction.OUTGOING).
                add(GROUPED_TO_PARENT, Direction.OUTGOING).
                add(GROUPED_TO_CHILD, Direction.OUTGOING).
                addRelationshipFilter(routeFilter).
                build();
//
//        return PathExpanders.forTypesAndDirections(
//                ON_ROUTE, Direction.OUTGOING,
//                STATION_TO_ROUTE, Direction.OUTGOING,
//                ROUTE_TO_STATION, Direction.OUTGOING,
//                WALKS_TO, Direction.OUTGOING,
//                WALKS_FROM, Direction.OUTGOING,
//                NEIGHBOUR, Direction.OUTGOING,
//                GROUPED_TO_PARENT, Direction.OUTGOING,
//                GROUPED_TO_CHILD, Direction.OUTGOING
//        );
    }

    public int costToInterchange(Transaction txn, RouteStation routeStation) {
        if (interchangeRepository.isInterchange(routeStation.getStation())) {
            return 0;
        }
        return  findCostToInterchange(txn, routeStation);
    }

    private int findCostToInterchange(Transaction txn, RouteStation routeStation) {

        logger.debug("Find cost to first interchange for " + routeStation);

        String query = "MATCH path = (start:ROUTE_STATION {route_station_id: $routeStationId})-[:ON_ROUTE*]->(inter:INTERCHANGE) " +
            " RETURN path ";

        Map<String, Object> params = new HashMap<>();
        params.put("routeStationId", routeStation.getId().getGraphId());

        Result results = txn.execute(query, params);

        OptionalInt maybeMin = results.stream().
                filter(row -> row.containsKey("path")).
                map(row -> (Path) row.get("path")).
                mapToInt(this::costFor).
                min();

        return maybeMin.orElse(-1);
    }

    private int costFor(Path path) {
        return Streams.stream(path.iterator()).
                filter(entity -> entity.hasProperty(COST.getText())).
                mapToInt(entity -> (int) entity.getProperty(COST.getText())).
                sum();
    }


}
