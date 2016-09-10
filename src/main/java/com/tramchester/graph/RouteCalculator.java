package com.tramchester.graph;

import com.tramchester.domain.*;
import com.tramchester.domain.exceptions.TramchesterException;
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

    public static final int MAX_NUM_GRAPH_PATHS = 2; // todo into config

    private String queryNodeName = "BEGIN";

    private NodeFactory nodeFactory;
    private PathExpander pathExpander;
    private MapPathToStages pathToStages;
    private CostEvaluator<Double> costEvaluator;

    public RouteCalculator(GraphDatabaseService db, NodeFactory nodeFactory, RelationshipFactory relationshipFactory,
                           SpatialDatabaseService spatialDatabaseService, PathExpander pathExpander, MapPathToStages pathToStages, CostEvaluator<Double> costEvaluator) {
        super(db, relationshipFactory, spatialDatabaseService, true);
        this.nodeFactory = nodeFactory;
        this.pathExpander = pathExpander;
        this.pathToStages = pathToStages;
        this.costEvaluator = costEvaluator;
    }

    public Set<RawJourney> calculateRoute(String startStationId, String endStationId, List<Integer> queryTimes,
                                          TramServiceDate queryDate) throws TramchesterException {
        Set<RawJourney> journeys = new LinkedHashSet<>(); // order matters
        Node endNode = getStationNode(endStationId);


        try (Transaction tx = graphDatabaseService.beginTx()) {
            Node startNode = getStationNode(startStationId);

            gatherJounerys(endNode, queryTimes, queryDate, journeys, startNode, MAX_NUM_GRAPH_PATHS);

            tx.success();
        }
        return journeys;
    }

    public Set<RawJourney> calculateRoute(LatLong origin, List<StationWalk> startStations, Station endStation,
                                          List<Integer> queryTimes, TramServiceDate queryDate) throws TramchesterException {
        Set<RawJourney> journeys = new LinkedHashSet<>(); // order matters
        int limit = startStations.isEmpty() ? MAX_NUM_GRAPH_PATHS : (MAX_NUM_GRAPH_PATHS*startStations.size());
        Node endNode = getStationNode(endStation.getId());

        try (Transaction tx = graphDatabaseService.beginTx()) {

            Node startNode = graphDatabaseService.createNode(DynamicLabel.label("QUERY_NODE"));
            startNode.setProperty(GraphStaticKeys.Station.LAT, origin.getLat());
            startNode.setProperty(GraphStaticKeys.Station.LONG, origin.getLon());
            startNode.setProperty(GraphStaticKeys.Station.NAME, queryNodeName);
            startNode.setProperty(GraphStaticKeys.STATION_TYPE, GraphStaticKeys.QUERY);
            logger.info(format("Added start node at %s as node %s", origin, startNode));

            startStations.forEach(stationWalk -> {
                String id = stationWalk.getId();
                Node node = getStationNode(id);
                int cost = stationWalk.getCost();
                logger.info(format("Add walking relationship from %s to %s cost %s", startNode, id, cost));
                startNode.createRelationshipTo(node, TransportRelationshipTypes.WALKS_TO).
                        setProperty(GraphStaticKeys.COST, cost);
            });

            gatherJounerys(endNode, queryTimes, queryDate, journeys, startNode, limit);

            startNode.delete();

            // no commit
            tx.close();
        }
        return journeys;
    }

    private void gatherJounerys(Node endNode, List<Integer> queryTimes, TramServiceDate queryDate, Set<RawJourney> journeys,
                                Node startNode, int limit) throws TramchesterException {
        queryTimes.forEach(queryTime->{
            Stream<WeightedPath> paths = findShortestPath(startNode, endNode, queryTime, queryDate);
            mapStreamToJourneySet(journeys, paths, limit, queryTime);
        });
    }

    private void mapStreamToJourneySet(Set<RawJourney> journeys, Stream<WeightedPath> paths,
                                       int limit, int minsPathMidnight) {
        paths.limit(limit).forEach(path->{
            logger.info(format("map graph path of length %s with limit of %s ",path.length(), limit));
            List<TransportStage> stages = pathToStages.map(path, minsPathMidnight);
            RawJourney journey = new RawJourney(stages, minsPathMidnight);
            journeys.add(journey);
        });
        paths.close();
        if (journeys.size()<1) {
            logger.warn(format("Did not create any journeys from the limit:%s queryTime:%s",
                    limit, minsPathMidnight));
        }
    }

    public TramNode getStation(String id) {
        Node node =  getStationNode(id);
        return nodeFactory.getNode(node);
    }

    private Stream<WeightedPath> findShortestPath(Node startNode, Node endNode, int queryTime, TramServiceDate queryDate) {
        logger.info(format("Finding shortest path for %s --> %s on %s at %s",
                startNode.getProperty(GraphStaticKeys.Station.NAME),
                endNode.getProperty(GraphStaticKeys.Station.NAME), queryDate, queryTime));

        GraphBranchState state = new GraphBranchState(queryDate, queryTime);

        if (startNode.getProperty(GraphStaticKeys.Station.NAME).equals(queryNodeName)) {
            logger.info("Query node based search, setting start time to actual query time");
            state.setStartTime(queryTime);
        }

        InitialBranchState.State<GraphBranchState> stateFactory = new InitialBranchState.State<>(state, state);
        PathFinder<WeightedPath> pathFinder = GraphAlgoFactory.dijkstra(
                pathExpander,
                stateFactory,
                costEvaluator);

        Iterable<WeightedPath> pathIterator = pathFinder.findAllPaths(startNode, endNode);
        return StreamSupport.stream(pathIterator.spliterator(), false);
    }

    public List<TransportRelationship> getOutboundRouteStationRelationships(String routeStationId) throws TramchesterException {
        return graphQuery.getRouteStationRelationships(routeStationId, Direction.OUTGOING);
    }

    public List<TransportRelationship> getInboundRouteStationRelationships(String routeStationId) throws TramchesterException {
        return graphQuery.getRouteStationRelationships(routeStationId, Direction.INCOMING);
    }

}
