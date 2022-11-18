package com.tramchester.graph;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.time.InvalidDurationException;
import com.tramchester.domain.dates.TramServiceDate;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.repository.RouteRepository;
import org.neo4j.graphalgo.EvaluationContext;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphalgo.impl.util.DoubleEvaluator;
import org.neo4j.graphdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.Duration;
import java.time.LocalDate;
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
    private final RouteRepository routeRepository;

    @Inject
    public RouteCostCalculator(GraphQuery graphQuery, GraphDatabase graphDatabaseService,
                               StagedTransportGraphBuilder.Ready ready, RouteRepository routeRepository) {
        this.graphQuery = graphQuery;
        this.graphDatabaseService = graphDatabaseService;
        this.routeRepository = routeRepository;
    }

    public Duration getAverageCostBetween(Transaction txn, Node startNode, Node endNode, TramDate date) throws InvalidDurationException {
        return calculateLeastCost(txn, startNode, endNode, COST, date);
    }

    public Duration getAverageCostBetween(Transaction txn, Location<?> station, Node endNode, TramDate date) throws InvalidDurationException {
        Node startNode = graphQuery.getLocationNode(txn, station);
        return calculateLeastCost(txn, startNode, endNode, COST, date);
    }

    // startNode must have been found within supplied txn
    public Duration getAverageCostBetween(Transaction txn, Node startNode, Location<?> endStation, TramDate date) throws InvalidDurationException {
        Node endNode = graphQuery.getLocationNode(txn, endStation);
        return calculateLeastCost(txn, startNode, endNode, COST, date);
    }

    public Duration getAverageCostBetween(Transaction txn, Location<?> startStation, Location<?> endStation, TramDate date) throws InvalidDurationException {
        return getCostBetween(txn, startStation, endStation, COST, date);
    }

    public Duration getMaxCostBetween(Transaction txn, Location<?> start, Location<?> end, TramDate date) throws InvalidDurationException {
        return getCostBetween(txn, start, end, MAX_COST, date);
    }

    public Duration getMaxCostBetween(Transaction txn, Node start, Location<?> endStation, TramDate date) throws InvalidDurationException {
        Node endNode = graphQuery.getLocationNode(txn, endStation);
        return calculateLeastCost(txn, start, endNode, MAX_COST, date);
    }

    private Duration getCostBetween(Transaction txn, Location<?> startLocation, Location<?> endLocation, GraphPropertyKey key, TramDate date) throws InvalidDurationException {
        Node startNode = graphQuery.getLocationNode(txn, startLocation);
        if (startNode==null) {
            throw new RuntimeException("Could not find start node for graph id " + startLocation.getId().getGraphId());
        }
        Node endNode = graphQuery.getLocationNode(txn, endLocation);
        if (endNode==null) {
            throw new RuntimeException("Could not find end node for graph id" + endLocation.getId().getGraphId());
        }
        logger.info(format("Find approx. route cost between %s and %s", startLocation.getId(), endLocation.getId()));

        return calculateLeastCost(txn, startNode, endNode, key, date);
    }

    // startNode and endNode must have been found within supplied txn

    private Duration calculateLeastCost(Transaction txn, Node startNode, Node endNode, GraphPropertyKey key, TramDate date) throws InvalidDurationException {
        IdSet<Route> running = IdSet.from(routeRepository.getRoutesRunningOn(date));
        // TODO fetch all the relationshipIds first

        EvaluationContext context = graphDatabaseService.createContext(txn);

        Predicate<? super Relationship> routeFilter = (Predicate<Relationship>) relationship ->
                !relationship.isType(ON_ROUTE) || running.contains(GraphProps.getRouteIdFrom(relationship));

        PathExpander<Double> forTypesAndDirections = fullExpanderForCostApproximation(routeFilter);

        PathFinder<WeightedPath> finder = GraphAlgoFactory.dijkstra(context, forTypesAndDirections,
                new UsefulLoggingCostEvaluator(key));

        WeightedPath path = finder.findSinglePath(startNode, endNode);
        if (path==null) {
            final String message = format("No (least cost) path found between node %s [%s] and node %s [%s]",
                    startNode.getId(), startNode.getAllProperties(), endNode.getId(), endNode.getAllProperties());
            logger.error(message);
            throw new InvalidDurationException(message);
        }
        double weight  = Math.floor(path.weight());

        int value = (int) weight;
        return Duration.ofMinutes(value);
    }

    private PathExpander<Double> fullExpanderForCostApproximation(Predicate<? super Relationship> routeFilter) {
        return PathExpanderBuilder.empty().
                add(ON_ROUTE, Direction.OUTGOING).
                add(STATION_TO_ROUTE, Direction.OUTGOING).
                add(ROUTE_TO_STATION, Direction.OUTGOING).
                add(WALKS_TO_STATION, Direction.OUTGOING).
                add(WALKS_FROM_STATION, Direction.OUTGOING).
                add(NEIGHBOUR, Direction.OUTGOING).
                add(GROUPED_TO_PARENT, Direction.OUTGOING).
                add(GROUPED_TO_CHILD, Direction.OUTGOING).
                addRelationshipFilter(routeFilter).
                build();

    }

    private static class UsefulLoggingCostEvaluator extends DoubleEvaluator {

        // default implementation gives zero useful diagnostics, just throws NotFoundException

        private final String name;

        public UsefulLoggingCostEvaluator(GraphPropertyKey costProperty) {
            super(costProperty.getText());
            this.name = costProperty.getText();
        }

        @Override
        public Double getCost(Relationship relationship, Direction direction) {
            try {
                return super.getCost(relationship, direction);
            }
            catch (NotFoundException exception) {
                final String msg = format("%s not found for %s dir %s type %s props %s",
                        name, relationship.getId(), direction, relationship.getType(), relationship.getAllProperties());
                logger.error(msg);
                throw new RuntimeException(msg, exception);
            }
        }
    }


}
