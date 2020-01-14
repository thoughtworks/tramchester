package com.tramchester.integration.graph;


import com.tramchester.Dependencies;
import com.tramchester.DiagramCreator;
import com.tramchester.domain.Location;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.integration.Stations;
import org.junit.*;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.junit.Assume.assumeFalse;

public class CreateDotDiagramTest {
    private static Dependencies dependencies;
    private GraphDatabaseService graphService;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        IntegrationTramTestConfig configuration = new IntegrationTramTestConfig();
        dependencies.initialise(configuration);
    }

    @Before
    public void beforeEachOfTheTestsRun() {
        graphService = dependencies.get(GraphDatabaseService.class);
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Test
    public void shouldProduceADotDiagramOfTheTramNetwork() throws IOException {
        int depthLimit = 2;

        create(Stations.Deansgate, depthLimit);
        create(Stations.StPetersSquare, depthLimit);
        create(Stations.Cornbrook, depthLimit);
        create(Stations.ExchangeSquare, depthLimit);
        create(Stations.MarketStreet, depthLimit);
        create(Stations.Victoria, depthLimit);
        create(Arrays.asList(Stations.ExchangeSquare,Stations.Deansgate,Stations.Cornbrook,Stations.ExchangeSquare), 4);
    }

    public void create(List<Location> startPoints, int depthLimit) throws IOException {
        String filename = startPoints.get(0).getName();
        DiagramCreator creator = new DiagramCreator(graphService, depthLimit);
        List<String> ids = startPoints.stream().map(point -> point.getId()).collect(Collectors.toList());
        creator.create(format("around_%s_trams.dot", filename), ids);
    }

    public void create(Location startPoint, int depthLimit) throws IOException {
        DiagramCreator creator = new DiagramCreator(graphService, depthLimit);
        creator.create(format("%s_trams.dot", startPoint.getName()), startPoint.getId());
    }

}
