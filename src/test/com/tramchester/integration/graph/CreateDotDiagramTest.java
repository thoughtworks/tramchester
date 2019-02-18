package com.tramchester.integration.graph;


import com.tramchester.Dependencies;
import com.tramchester.DiagramCreator;
import com.tramchester.domain.Location;
import com.tramchester.graph.Nodes.NodeFactory;
import com.tramchester.graph.Relationships.RelationshipFactory;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.integration.Stations;
import org.junit.*;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.IOException;

import static java.lang.String.format;

public class CreateDotDiagramTest {
    private static Dependencies dependencies;
    private GraphDatabaseService graphService;
    private RelationshipFactory relationshipFactory;
    private NodeFactory nodeFactory;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        dependencies.initialise(new IntegrationTramTestConfig());
    }

    @Before
    public void beforeEachOfTheTestsRun() {
        graphService = dependencies.get(GraphDatabaseService.class);
        relationshipFactory = dependencies.get(RelationshipFactory.class);
        nodeFactory = dependencies.get(NodeFactory.class);
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Test
    public void shouldProduceADotDiagramOfTheTramNetwork() throws IOException {
        int depthLimit = 7;

        create(Stations.Deansgate, depthLimit);
    }

    // media city is area somewhat unique....
    @Test
    public void shouldProduceADotDiagramOfTheTramNetworkForMediaCityArea() throws IOException {
        int depthLimit = 3;
        create(Stations.MediaCityUK, depthLimit);
        create(Stations.HarbourCity, depthLimit);
        create(Stations.Broadway, depthLimit);
    }

    public void create(Location startPoint, int depthLimit) throws IOException {
        DiagramCreator creator = new DiagramCreator(nodeFactory, relationshipFactory, graphService, depthLimit);
        creator.create(format("%s_trams.dot", startPoint.getName()), startPoint.getId());
    }

}
