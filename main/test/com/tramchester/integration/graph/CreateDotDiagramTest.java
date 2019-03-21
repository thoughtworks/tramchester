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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

    @Test
    public void shouldProduceADotDiagramOfTheTramNetworkForMediaCityArea() throws IOException {
        int depthLimit = 3;
        create(Stations.MediaCityUK, depthLimit);
        create(Stations.HarbourCity, depthLimit);
        create(Stations.Piccadilly, depthLimit);

        create(Arrays.asList(
                new Location[]{Stations.StPetersSquare,Stations.Deansgate,Stations.Cornbrook,Stations.Pomona }), 5);
    }


    public void create(List<Location> startPoints, int depthLimit) throws IOException {
        String filename = startPoints.get(0).getName();
        DiagramCreator creator = new DiagramCreator(nodeFactory, relationshipFactory, graphService, depthLimit);
        List<String> ids = startPoints.stream().map(point -> point.getId()).collect(Collectors.toList());
        creator.create(format("around_%s_trams.dot", filename), ids);
    }

    public void create(Location startPoint, int depthLimit) throws IOException {
        DiagramCreator creator = new DiagramCreator(nodeFactory, relationshipFactory, graphService, depthLimit);
        creator.create(format("%s_trams.dot", startPoint.getName()), startPoint.getId());
    }

}
