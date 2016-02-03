package com.tramchester.graph;

import com.tramchester.domain.*;
import com.tramchester.domain.exceptions.UnknownStationException;
import com.tramchester.graph.Nodes.NodeFactory;
import com.tramchester.graph.Nodes.TramNode;
import com.tramchester.graph.Relationships.RelationshipFactory;
import com.tramchester.graph.Relationships.TransportRelationship;
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

    // TODO Use INT? - but difficult as not supported by algos built into neo4j
    public static final CostEvaluator<Double> COST_EVALUATOR = CommonEvaluators.doubleCostEvaluator(GraphStaticKeys.COST);

    private NodeFactory nodeFactory;
    private PathExpander pathExpander;
    private PathToStagesMapper pathToStages;

    public RouteCalculator(GraphDatabaseService db, NodeFactory nodeFactory, RelationshipFactory relationshipFactory,
                           PathToStagesMapper pathToStages) {
        super(db, relationshipFactory, true);
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

    public Set<RawJourney> calculateRoute(List<StationWalk> startStations, List<Station> endStations, int minutesFromMidnight,
                                          DaysOfWeek dayOfWeek, TramServiceDate queryDate) throws UnknownStationException {
        Set<RawJourney> journeys = new LinkedHashSet<>(); // order matters
        try (Transaction tx = graphDatabaseService.beginTx()) {

            Node startNode = graphDatabaseService.createNode(DynamicLabel.label("QUERY_NODE"));
            startNode.setProperty(GraphStaticKeys.Station.NAME, "BEGIN");
            startNode.setProperty(GraphStaticKeys.STATION_TYPE, GraphStaticKeys.QUERY);

            startStations.forEach(station -> {
                Node node = getStationNode(station.getId());
                startNode.createRelationshipTo(node, TransportRelationshipTypes.WALKS_TO).
                        setProperty(GraphStaticKeys.COST, station.getCost());
            });

            Node endNode = graphDatabaseService.createNode(DynamicLabel.label("QUERY_NODE"));
            endNode.setProperty(GraphStaticKeys.Station.NAME, "END");
            endNode.setProperty(GraphStaticKeys.STATION_TYPE, GraphStaticKeys.QUERY);

            endStations.forEach(station -> {
                Node penulimate = getStationNode(station.getId());
                penulimate.createRelationshipTo(endNode, TransportRelationshipTypes.WALKS_TO).
                        setProperty(GraphStaticKeys.COST, 0);
            });

            Stream<WeightedPath> paths = findShortestPath(startNode, endNode, minutesFromMidnight, dayOfWeek, queryDate);

            int limit = MAX_NUM_GRAPH_PATHS * startStations.size();
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
            logger.info(format("Map graph path of length %s with limit of %s ",path.length(), limit));
            List<RawStage> stages = pathToStages.mapStages(path, minsPathMidnight);
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
        logger.info(format("Finding shortest path for %s --> %s on %s",
                startNode.getProperty(GraphStaticKeys.Station.NAME),
                endNode.getProperty(GraphStaticKeys.Station.NAME), dayOfWeek));

        GraphBranchState state = new GraphBranchState(dayOfWeek, queryDate, queryTime);
        InitialBranchState.State<GraphBranchState> stateFactory = new InitialBranchState.State<>(state, state);
        PathFinder<WeightedPath> pathFinder = GraphAlgoFactory.dijkstra(
                pathExpander,
                stateFactory,
                COST_EVALUATOR);

        Iterable<WeightedPath> pathIterator = pathFinder.findAllPaths(startNode, endNode);
        return StreamSupport.stream(pathIterator.spliterator(), false);
    }

    public List<TransportRelationship> getOutboundRouteStationRelationships(String routeStationId) throws UnknownStationException {
        return graphQuery.getRouteStationRelationships(routeStationId, Direction.OUTGOING);
    }

    public List<TransportRelationship> getInboundRouteStationRelationships(String routeStationId) throws UnknownStationException {
        return graphQuery.getRouteStationRelationships(routeStationId, Direction.INCOMING);
    }

    public List<TransportRelationship> getOutboundStationRelationships(String stationId) throws UnknownStationException {
        return graphQuery.getStationRelationships(stationId, Direction.OUTGOING);
    }

}
