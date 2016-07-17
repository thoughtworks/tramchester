package com.tramchester.graph;

import com.tramchester.IntegrationTramTestConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.Nodes.NodeFactory;
import com.tramchester.graph.Relationships.PathToTransportRelationship;
import com.tramchester.graph.Relationships.RelationshipFactory;
import com.tramchester.resources.RouteCodeToClassMapper;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.gis.spatial.SpatialDatabaseService;
import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static junit.framework.TestCase.assertEquals;

public class GraphWithSimpleRouteTest {

    private static final String TMP_DB = "tmp.db";

    private static TransportDataForTest transportData;
    private static RouteCalculator calculator;
    private TramServiceDate queryDate;
    private int queryTime;

    @BeforeClass
    public static void onceBeforeAllTestRuns() throws IOException {
        File dbFile = new File(TMP_DB);
        FileUtils.deleteDirectory(dbFile);
        GraphDatabaseFactory graphDatabaseFactory = new GraphDatabaseFactory();
        GraphDatabaseService graphDBService = graphDatabaseFactory.newEmbeddedDatabase(dbFile);

        NodeFactory nodeFactory = new NodeFactory();
        RelationshipFactory relationshipFactory = new RelationshipFactory(nodeFactory);

        transportData = new TransportDataForTest();
        SpatialDatabaseService spatialDatabaseService = new SpatialDatabaseService(graphDBService);
        TransportGraphBuilder builder = new TransportGraphBuilder(graphDBService, transportData, relationshipFactory, spatialDatabaseService);
        builder.buildGraph();

        RouteCodeToClassMapper routeIdToClass = new RouteCodeToClassMapper();
        PathToTransportRelationship pathToRelationships  = new PathToTransportRelationship(relationshipFactory);
        MapTransportRelationshipsToStages relationshipsToStages = new MapTransportRelationshipsToStages(routeIdToClass, transportData);

        CostEvaluator<Double> costEvaluator = new CachingCostEvaluator();
        TramchesterConfig configuration = new IntegrationTramTestConfig();
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, configuration);
        TimeBasedPathExpander pathExpander = new TimeBasedPathExpander(relationshipFactory, nodeFactory, serviceHeuristics);

        MapPathToStages mapper = new MapPathToStages(pathToRelationships, relationshipsToStages);
        calculator = new RouteCalculator(graphDBService, nodeFactory, relationshipFactory,
                spatialDatabaseService, pathExpander, mapper, costEvaluator);
    }

    @Before
    public void beforeEachTestRuns() {
        queryDate = new TramServiceDate("20140630");
        queryTime = (8 * 60)-3;
    }

    @Test
    public void shouldTestSimpleJourneyIsPossible() throws TramchesterException {
        Set<RawJourney> journeys = calculator.calculateRoute(TransportDataForTest.FIRST_STATION,
                TransportDataForTest.SECOND_STATION, queryTime, queryDate);
        assertEquals(1, journeys.size());
    }

    @Test
    public void shouldTestSimpleJourneyIsPossibleToInterchange() throws TramchesterException {
        Set<RawJourney> journeys = calculator.calculateRoute(TransportDataForTest.FIRST_STATION,
                TransportDataForTest.INTERCHANGE, queryTime, queryDate);
        assertEquals(1, journeys.size());
    }

    @Test
    public void shouldTestSimpleJourneyIsNotPossible() throws TramchesterException {
        Set<RawJourney> journeys = calculator.calculateRoute(TransportDataForTest.FIRST_STATION,
                TransportDataForTest.INTERCHANGE, 9*60, queryDate);
        assertEquals(0, journeys.size());
    }

    @Test
    public void shouldTestJourneyEndOverWaitLimitIsPossible() throws TramchesterException {
        Set<RawJourney> journeys = calculator.calculateRoute(TransportDataForTest.FIRST_STATION,
                TransportDataForTest.LAST_STATION, queryTime, queryDate);
        assertEquals(1, journeys.size());
    }

    @Test
    public void shouldTestJourneyEndOverWaitLimitViaInterchangeIsPossible() throws TramchesterException {
        Set<RawJourney> journeys = calculator.calculateRoute(TransportDataForTest.FIRST_STATION,
                TransportDataForTest.STATION_FOUR, queryTime, queryDate);
        assertEquals(1, journeys.size());
    }

}
