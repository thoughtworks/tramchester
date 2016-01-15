package com.tramchester.graph;

import com.tramchester.domain.DaysOfWeek;
import com.tramchester.domain.RawJourney;
import com.tramchester.domain.RawStage;
import com.tramchester.domain.exceptions.UnknownStationException;
import com.tramchester.domain.TramServiceDate;
import com.tramchester.graph.Nodes.NodeFactory;
import com.tramchester.graph.Nodes.RouteStationNode;
import com.tramchester.graph.Nodes.StationNode;
import com.tramchester.graph.Nodes.TramNode;
import com.tramchester.graph.Relationships.RelationshipFactory;
import com.tramchester.graph.Relationships.TramGoesToRelationship;
import com.tramchester.graph.Relationships.TramRelationship;
import com.tramchester.resources.RouteCodeToClassMapper;
import org.neo4j.graphalgo.*;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.InitialBranchState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.tramchester.graph.GraphStaticKeys.COST;
import static com.tramchester.graph.GraphStaticKeys.QUERY;
import static com.tramchester.graph.GraphStaticKeys.STATION_TYPE;
import static com.tramchester.graph.GraphStaticKeys.Station.NAME;
import static java.lang.String.format;

public class RouteCalculator extends StationIndexs {
    private static final Logger logger = LoggerFactory.getLogger(RouteCalculator.class);

    public static final int MAX_WAIT_TIME_MINS = 25; // todo into config

    // TODO Use INT
    public static final CostEvaluator<Double> COST_EVALUATOR = CommonEvaluators.doubleCostEvaluator(COST);
    public static final int MAX_NUM_GRAPH_PATHS = 2;

    private NodeFactory nodeFactory;
    private PathExpander pathExpander;
    private RouteCodeToClassMapper routeCodeToClassMapper;

    public RouteCalculator(GraphDatabaseService db, NodeFactory nodeFactory, RelationshipFactory relationshipFactory,
                           RouteCodeToClassMapper routeCodeToClassMapper) {
        super(db, true);
        this.nodeFactory = nodeFactory;
        this.routeCodeToClassMapper = routeCodeToClassMapper;
        pathExpander = new TimeBasedPathExpander(COST_EVALUATOR, MAX_WAIT_TIME_MINS, relationshipFactory, nodeFactory);
    }

    public Set<RawJourney> calculateRoute(String startStationId, String endStationId, int queryTime, DaysOfWeek dayOfWeek,
                                       TramServiceDate queryDate) throws UnknownStationException {
        Set<RawJourney> journeys = new LinkedHashSet<>(); // order matters

        try (Transaction tx = graphDatabaseService.beginTx()) {
            Node startNode = getStationNode(startStationId);
            Node endNode = getStationNode(endStationId);

            Stream<WeightedPath> paths = findShortestPath(startNode, endNode, queryTime, dayOfWeek, queryDate);
            // todo eliminate duplicate journeys that use different services??

            mapStreamToJourneySet(journeys, paths, MAX_NUM_GRAPH_PATHS, queryTime);
            tx.success();
        }
        return journeys;
    }

    public Set<RawJourney> calculateRoute(List<Node> starts, List<Node> ends, int minutesFromMidnight,
                                          DaysOfWeek dayOfWeek, TramServiceDate queryDate) throws UnknownStationException {
        Set<RawJourney> journeys = new LinkedHashSet<>(); // order matters
        try (Transaction tx = graphDatabaseService.beginTx()) {
            Node startNode = graphDatabaseService.createNode(DynamicLabel.label("QUERY_NODE"));
            startNode.setProperty(NAME, "BEGIN");
            startNode.setProperty(STATION_TYPE, QUERY);
            starts.forEach(start -> startNode.
                    createRelationshipTo(start, TransportRelationshipTypes.WALKS_TO).
                    setProperty(COST,0));

            Node endNode = graphDatabaseService.createNode(DynamicLabel.label("QUERY_NODE"));
            endNode.setProperty(NAME, "END");
            endNode.setProperty(STATION_TYPE, QUERY);
            ends.forEach(end -> end.
                    createRelationshipTo(endNode, TransportRelationshipTypes.WALKS_TO).
                    setProperty(COST,0));

            Stream<WeightedPath> paths = findShortestPath(startNode, endNode, minutesFromMidnight, dayOfWeek, queryDate);

            int limit = MAX_NUM_GRAPH_PATHS * starts.size();
            mapStreamToJourneySet(journeys, paths, limit, minutesFromMidnight);

            startNode.delete();
            endNode.delete();

            tx.close();
        }
        return journeys;
    }

    private void mapStreamToJourneySet(Set<RawJourney> journeys, Stream<WeightedPath> paths,
                                       int limit, int minsPathMidnight) {
        paths.limit(limit).forEach(path->{
            logger.info("Map graph path of length " + path.length());
            List<RawStage> stages = mapStages(path, minsPathMidnight);
            int index = journeys.size();
            RawJourney journey = new RawJourney(stages, index);
            journeys.add(journey);
        });
    }

    private List<RawStage> mapStages(WeightedPath path, int minsPastMidnight) {
        List<RawStage> stages = new ArrayList<>();
        RawStage currentStage = null;

        Iterable<Relationship> relationships = path.relationships();
        RelationshipFactory relationshipFactory = new RelationshipFactory();

        logger.info("Mapping path to stages, weight is " + path.weight());

        int totalCost = 0;
        for (Relationship graphRelationship : relationships) {
            TramRelationship tramRelationship = relationshipFactory.getRelationship(graphRelationship);

            TramNode startNode = nodeFactory.getNode(graphRelationship.getStartNode());
            String startNodeId = startNode.getId();

            TramNode endNode = nodeFactory.getNode(graphRelationship.getEndNode());
            String endNodeId = endNode.getId();

            int cost = tramRelationship.getCost();
            totalCost += cost;

            // todo refactor out first and subsequent stage handling
            int elapsedTime = minsPastMidnight + totalCost;
            if (tramRelationship.isBoarding()) {
                // station -> route station
                RouteStationNode routeStationNode = (RouteStationNode) endNode;
                String routeName = routeStationNode.getRouteName();
                String routeId = routeStationNode.getRouteId();
                logger.info(format("Board tram: at:'%s' from '%s' at %s", endNode, startNode, elapsedTime));

                String tramRouteClass = routeCodeToClassMapper.map(routeId);

                currentStage = new RawStage(startNodeId, routeName, tramRelationship.getMode(), tramRouteClass);
            } else if (tramRelationship.isTramGoesTo()) {
                // routeStation -> routeStation
                TramGoesToRelationship tramGoesToRelationship = (TramGoesToRelationship) tramRelationship;
                String serviceId = tramGoesToRelationship.getService();
                logger.info(format("Add goes to %s, service %s, elapsed %s", tramGoesToRelationship.getDest(),
                        serviceId, elapsedTime));
                currentStage.setServiceId(serviceId);
            } else if (tramRelationship.isDepartTram()) {
                // route station -> station
                StationNode stationNode = (StationNode) endNode;
                String stationName = stationNode.getName();
                logger.info(format("Depart tram: at:'%s' to: '%s' '%s' at %s", startNodeId, stationName, endNodeId,
                        elapsedTime));
                currentStage.setLastStation(endNodeId);
                logger.info(format("Added stage: '%s' at time %s",currentStage, elapsedTime));
                stages.add(currentStage);
            } else if (tramRelationship.isWalk()) {
                logger.info("Skip adding walk of cost " + cost);
            }
        }
        logger.info(format("Number of stages: %s Total cost:%s Finish: %s",stages.size(), totalCost,
                totalCost+minsPastMidnight));
        return stages;
    }

    public TramNode getStation(String id) throws UnknownStationException {
        NodeFactory factory = new NodeFactory();
        Node node =  getStationNode(id);
        return factory.getNode(node);
    }

    public TramNode getRouteStation(String id) throws UnknownStationException {
        NodeFactory factory = new NodeFactory();
        Node node = getRouteStationNode(id);
        return factory.getNode(node);
    }

    private Stream<WeightedPath> findShortestPath(Node startNode, Node endNode, int queryTime, DaysOfWeek dayOfWeek,
                                                    TramServiceDate queryDate) {
        logger.info(format("Finding shortest path for %s --> %s on %s",
                startNode.getProperty(NAME),
                endNode.getProperty(NAME), dayOfWeek));

        GraphBranchState state = new GraphBranchState(dayOfWeek, queryDate, queryTime);
        PathFinder<WeightedPath> pathFinder = GraphAlgoFactory.dijkstra(
                pathExpander,
                new InitialBranchState.State<>(state, state),
                COST_EVALUATOR);

        Iterable<WeightedPath> pathIterator = pathFinder.findAllPaths(startNode, endNode);
        return StreamSupport.stream(pathIterator.spliterator(), false);
    }

    public List<TramRelationship> getOutboundRouteStationRelationships(String routeStationId) throws UnknownStationException {
        return graphQuery.getRouteStationRelationships(routeStationId, Direction.OUTGOING);
    }


    public List<TramRelationship> getInboundRouteStationRelationships(String routeStationId) throws UnknownStationException {
        return graphQuery.getRouteStationRelationships(routeStationId, Direction.INCOMING);
    }

    public List<TramRelationship> getOutboundStationRelationships(String stationId) throws UnknownStationException {
        return graphQuery.getStationRelationships(stationId, Direction.OUTGOING);
    }

}
