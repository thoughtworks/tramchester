package com.tramchester.unit.graph;


import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.GraphQuery;
import com.tramchester.graph.GraphStaticKeys;
import com.tramchester.graph.NodeIdLabelMap;
import com.tramchester.graph.Nodes.NodeFactory;
import com.tramchester.graph.Relationships.RelationshipFactory;
import com.tramchester.graph.TransportGraphBuilder;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.gis.spatial.SpatialDatabaseService;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

public class GraphQueryTest {

    private static final String TMP_DB = "graph_query_test.db";
    private static GraphQuery graphQuery;
    private static GraphDatabaseService graphDBService;
    private static File dbFile;
    private static NodeFactory nodeFactory;
    private static NodeIdLabelMap nodeLabelIdMap;

    @BeforeClass
    public static void onceBeforeAllTestRuns() throws IOException, TramchesterException {
        dbFile = new File(TMP_DB);
        FileUtils.deleteDirectory(dbFile);
        GraphDatabaseFactory graphDatabaseFactory = new GraphDatabaseFactory();
        graphDBService = graphDatabaseFactory.newEmbeddedDatabase(dbFile);

        SpatialDatabaseService spatialDatabaseService = new SpatialDatabaseService(graphDBService);

        nodeFactory = new NodeFactory();
        RelationshipFactory relationshipFactory = new RelationshipFactory(nodeFactory);
        graphQuery = new GraphQuery(graphDBService, relationshipFactory, spatialDatabaseService);

        TransportDataForTest transportData = new TransportDataForTest();
        nodeLabelIdMap = new NodeIdLabelMap();
        TransportGraphBuilder builder = new TransportGraphBuilder(graphDBService, transportData, relationshipFactory,
                nodeLabelIdMap, graphQuery);
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
    public void shouldHaveCorrectEndNodesForRoute() {

        try (Transaction tx = graphDBService.beginTx()) {
            ArrayList<Node> nodes = graphQuery.findStartNodesFor("routeA");
            assertEquals(1, nodes.size());
            Node node = nodes.get(0);

            assertEquals(TransportDataForTest.FIRST_STATION+"MET:   1:I:",node.getProperty(GraphStaticKeys.ID).toString());
            tx.success();
        }
    }

}
