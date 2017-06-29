package com.tramchester.integration.graph;


import com.tramchester.integration.graph.Nodes.NodeFactory;
import com.tramchester.integration.graph.Relationships.RelationshipFactory;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.gis.spatial.SpatialDatabaseService;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.junit.Assert.assertEquals;

public class GraphQueryTest {

    private static final String TMP_DB = "graph_query_test.db";
    private static GraphQuery graphQuery;
    private static GraphDatabaseService graphDBService;
    private static File dbFile;
    private static NodeFactory nodeFactory;

    @BeforeClass
    public static void onceBeforeAllTestRuns() throws IOException {
        dbFile = new File(TMP_DB);
        FileUtils.deleteDirectory(dbFile);
        GraphDatabaseFactory graphDatabaseFactory = new GraphDatabaseFactory();
        graphDBService = graphDatabaseFactory.newEmbeddedDatabase(dbFile);

        SpatialDatabaseService spatialDatabaseService = new SpatialDatabaseService(graphDBService);

        nodeFactory = new NodeFactory();
        RelationshipFactory relationshipFactory = new RelationshipFactory(nodeFactory);
        graphQuery = new GraphQuery(graphDBService, relationshipFactory, spatialDatabaseService);

        TransportDataForTest transportData = new TransportDataForTest();
        TransportGraphBuilder builder = new TransportGraphBuilder(graphDBService, transportData, relationshipFactory,
                spatialDatabaseService);
        builder.buildGraph();
    }

    @AfterClass
    public static void afterAllOfTheTestsHaveRun() throws IOException {
        if (graphDBService!=null) {
            if (graphDBService.isAvailable(1)) {
                graphDBService.shutdown();
            }
        }
        FileUtils.deleteDirectory(dbFile);
    }

    @Test
    public void shouldHaveCorrectEndNodesForRoute() throws IOException {

        try (Transaction tx = graphDBService.beginTx()) {
            ArrayList<Node> nodes = graphQuery.findStartNodesFor("routeA");
            assertEquals(1, nodes.size());
            Node node = nodes.get(0);

            assertEquals(TransportDataForTest.FIRST_STATION+"routeAId",node.getProperty(GraphStaticKeys.ID).toString());
            tx.success();
        }
    }

    @Test
    public void shouldGetRouteStationsInCorrectOrder() {
        try (Transaction tx = graphDBService.beginTx()) {
            ResourceIterable<Node> nodes = graphQuery.getAllForRouteNoTx("routeA");
            Stream<Node> stream = StreamSupport.stream(nodes.spliterator(),false);
            List<String> list = stream.map(node -> node.getProperty(GraphStaticKeys.ID).toString()).collect(Collectors.toList());
            assertEquals(4,list.size());
            assertEquals(TransportDataForTest.FIRST_STATION+"routeAId", list.get(0));
            assertEquals(TransportDataForTest.SECOND_STATION+"routeAId", list.get(1));
            assertEquals(TransportDataForTest.INTERCHANGE+"routeAId", list.get(2));
            assertEquals(TransportDataForTest.LAST_STATION+"routeAId", list.get(3));
            tx.success();
        }

    }

}
