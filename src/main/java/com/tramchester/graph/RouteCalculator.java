package com.tramchester.graph;


import com.tramchester.domain.DaysOfWeek;
import org.neo4j.graphalgo.*;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.neo4j.graphdb.traversal.InitialBranchState;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Traversal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.neo4j.graphdb.DynamicRelationshipType.withName;

public class RouteCalculator {
    private static final Logger logger = LoggerFactory.getLogger(RouteCalculator.class);

    private static final PathExpander<Integer> pathExpander = new TripPathExpander(DaysOfWeek.fromToday(), 650);

    private static final TraversalDescription DELIVERY_BASE_FINDER = Traversal.description()
            .depthFirst()
            .evaluator(new Evaluator() {
                private final RelationshipType DELIVERY_ROUTE = withName("GOES_TO");

                @Override
                public Evaluation evaluate(Path path) {
                    if (isDeliveryBase(path)) {
                        return Evaluation.INCLUDE_AND_PRUNE;
                    }

                    return Evaluation.EXCLUDE_AND_CONTINUE;
                }

                private boolean isDeliveryBase(Path path) {
                    return !path.endNode().hasRelationship(DELIVERY_ROUTE, Direction.INCOMING);
                }
            });

    private static final CostEvaluator<Double> COST_EVALUATOR = CommonEvaluators.doubleCostEvaluator("cost");
    public static final Label LOCATION = DynamicLabel.label("Location");
    private GraphDatabaseService db;

    public RouteCalculator(GraphDatabaseService db) {
        this.db = db;
    }

    public Iterable<Node> calculateRoute(String start, String end, int interval) {
        try (Transaction tx = db.beginTx()) {
            logger.info("[x]-------[x] finding route!");
            // TraversalDescription deliveryBaseFinder = createDeliveryBaseFinder(interval);

//            Path upLeg = findRouteToDeliveryBase(start, deliveryBaseFinder);
//
//            System.out.printf("************************" + upLeg.toString());
//
//            Path downLeg = findRouteToDeliveryBase(end, deliveryBaseFinder);
//
//            System.out.printf("************************" + downLeg.toString());


            Iterable<WeightedPath> paths = findRouteBetweenDeliveryBases(
                    start,
                    end,
                    interval);
            for (WeightedPath path : paths) {
                printRoute(path);
                System.out.printf(path.toString());
            }
            //System.out.printf(topRoute.toString());

            //Set<Node> routes = combineRoutes(upLeg, downLeg, topRoute);
            tx.success();
            return null;
        }
    }

    private void printRoute(WeightedPath topRoute) {
        String path = "\n";

        Iterable<Relationship> relationships = topRoute.relationships();
        for (Relationship relationship : relationships) {
            if (topRoute.startNode().equals(relationship.getStartNode())) {
                path += String.format("(%s)", relationship.getStartNode().getProperty("name"));
            }
            path += "---" + relationship.getProperty("route") + "-" + relationship.getProperty("service_id") + "-->";
            path += String.format("(%s)", relationship.getEndNode().getProperty("name"));
        }
        path += "weight: " + topRoute.weight();
        System.out.println(path);
        System.out.println("------------------------------------------------------------------------------------");

    }

    private Iterable<WeightedPath> findRouteBetweenDeliveryBases(String start, String end, int interval) {
        Node startNode = getStationsIndex().get("id", start).getSingle();
        Node endNode = getStationsIndex().get("id", end).getSingle();

        PathFinder<WeightedPath> routeBetweenDeliveryBasesFinder = GraphAlgoFactory.dijkstra(
                pathExpander,
                new InitialBranchState.State<>(interval, interval),
                new TripCostEvaluator());

        return routeBetweenDeliveryBasesFinder.findAllPaths(startNode, endNode);
    }

    private TraversalDescription createDeliveryBaseFinder(int interval) {
        return DELIVERY_BASE_FINDER.expand(pathExpander,
                new InitialBranchState.State<>(interval, interval));
    }

    Index<Node> trams = null;

    private Index<Node> getStationsIndex() {
        if (trams == null) {
            trams = db.index().forNodes(GraphStaticKeys.Station.IndexName);
        }
        return trams;
    }

}
