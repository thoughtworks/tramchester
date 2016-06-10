package com.tramchester.graph;

import com.tramchester.domain.*;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.exceptions.UnknownStationException;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.graph.Nodes.NodeFactory;
import com.tramchester.graph.Nodes.TramNode;
import com.tramchester.graph.Relationships.RelationshipFactory;
import com.tramchester.graph.Relationships.TransportRelationship;
import org.neo4j.gis.spatial.SpatialDatabaseService;
import org.neo4j.graphalgo.*;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.InitialBranchState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.lang.String.format;

public class RouteCalculator extends StationIndexs {
    private static final Logger logger = LoggerFactory.getLogger(RouteCalculator.class);

    public static final int MAX_WAIT_TIME_MINS = 25; // todo into config
    public static final int MAX_NUM_GRAPH_PATHS = 2; // todo into config

    private String queryNodeName = "BEGIN";

    // TODO Use INT? - but difficult as not supported by algos built into neo4j
    //public static final CostEvaluator<Double> COST_EVALUATOR = CommonEvaluators.doubleCostEvaluator(GraphStaticKeys.COST);

    public static final CostEvaluator<Double> COST_EVALUATOR = new CachingCostEvaluator(GraphStaticKeys.COST);

    private NodeFactory nodeFactory;
    private PathExpander pathExpander;
    private PathToStagesMapper pathToStages;

    public RouteCalculator(GraphDatabaseService db, NodeFactory nodeFactory, RelationshipFactory relationshipFactory,
                           SpatialDatabaseService spatialDatabaseService, PathToStagesMapper pathToStages) {
        super(db, relationshipFactory, spatialDatabaseService, true);
        this.nodeFactory = nodeFactory;
        this.pathToStages = pathToStages;
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

    public Set<RawJourney> calculateRoute(LatLong origin, List<StationWalk> startStations, Station endStation, int minutesFromMidnight,
                                          DaysOfWeek dayOfWeek, TramServiceDate queryDate) throws UnknownStationException {
        Set<RawJourney> journeys = new LinkedHashSet<>(); // order matters
        try (Transaction tx = graphDatabaseService.beginTx()) {

            Node startNode = graphDatabaseService.createNode(DynamicLabel.label("QUERY_NODE"));
            startNode.setProperty(GraphStaticKeys.Station.LAT, origin.getLat());
            startNode.setProperty(GraphStaticKeys.Station.LONG, origin.getLon());
            startNode.setProperty(GraphStaticKeys.Station.NAME, queryNodeName);
            startNode.setProperty(GraphStaticKeys.STATION_TYPE, GraphStaticKeys.QUERY);

            startStations.forEach(stationWalk -> {
                String id = stationWalk.getId();
                Node node = getStationNode(id);
                int cost = stationWalk.getCost();
                logger.info(format("Add walking relationship from %s to %s cost %s", origin, id, cost));
                startNode.createRelationshipTo(node, TransportRelationshipTypes.WALKS_TO).
                        setProperty(GraphStaticKeys.COST, cost);
            });

            Node endNode = getStationNode(endStation.getId());

            Stream<WeightedPath> paths = findShortestPath(startNode, endNode, minutesFromMidnight, dayOfWeek, queryDate);

            int limit = MAX_NUM_GRAPH_PATHS * startStations.size();
            mapStreamToJourneySet(journeys, paths, limit, minutesFromMidnight);

            startNode.delete();

            // no commit
            tx.close();
        }
        return journeys;
    }

    private void mapStreamToJourneySet(Set<RawJourney> journeys, Stream<WeightedPath> paths,
                                       int limit, int minsPathMidnight) {
        paths.limit(limit).forEach(path->{
            logger.info(format("Map graph path of length %s with limit of %s ",path.length(), limit));
            List<TransportStage> stages = pathToStages.mapStages(path, minsPathMidnight);
            RawJourney journey = new RawJourney(stages);
            journeys.add(journey);
        });
    }


    public TramNode getStation(String id) throws UnknownStationException {
        Node node =  getStationNode(id);
        return nodeFactory.getNode(node);
    }

    public TramNode getRouteStation(String id) throws UnknownStationException {
        Node node = getRouteStationNode(id);
        return nodeFactory.getNode(node);
    }

    private Stream<WeightedPath> findShortestPath(Node startNode, Node endNode, int queryTime, DaysOfWeek dayOfWeek,
                                                    TramServiceDate queryDate) {
        logger.info(format("Finding shortest path for %s --> %s on %s at %s",
                startNode.getProperty(GraphStaticKeys.Station.NAME),
                endNode.getProperty(GraphStaticKeys.Station.NAME), dayOfWeek, queryTime));

        GraphBranchState state = new GraphBranchState(dayOfWeek, queryDate, queryTime);

        if (startNode.getProperty(GraphStaticKeys.Station.NAME).equals(queryNodeName)) {
            logger.info("Query node based search, setting start time to actual query time");
            state.setStartTime(queryTime);
        }

        InitialBranchState.State<GraphBranchState> stateFactory = new InitialBranchState.State<>(state, state);
        PathFinder<WeightedPath> pathFinder = GraphAlgoFactory.dijkstra(
                pathExpander,
                stateFactory,
                COST_EVALUATOR);

        Iterable<WeightedPath> pathIterator = pathFinder.findAllPaths(startNode, endNode);
        return StreamSupport.stream(pathIterator.spliterator(), false);
    }

    public List<TransportRelationship> getOutboundRouteStationRelationships(String routeStationId) throws TramchesterException {
        return graphQuery.getRouteStationRelationships(routeStationId, Direction.OUTGOING);
    }

    public List<TransportRelationship> getInboundRouteStationRelationships(String routeStationId) throws TramchesterException {
        return graphQuery.getRouteStationRelationships(routeStationId, Direction.INCOMING);
    }

    public List<TransportRelationship> getOutboundStationRelationships(String stationId) throws UnknownStationException {
        return graphQuery.getStationRelationships(stationId, Direction.OUTGOING);
    }

}
