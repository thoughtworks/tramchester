package com.tramchester.integration.graph;


import com.tramchester.Dependencies;
import com.tramchester.DiagramCreator;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.testSupport.Stations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static java.lang.String.format;

class CreateDotDiagramTest {
    private static Dependencies dependencies;
    private GraphDatabase database;

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        IntegrationTramTestConfig configuration = new IntegrationTramTestConfig();
        dependencies.initialise(configuration);
    }

    @BeforeEach
    void beforeEachOfTheTestsRun() {
        database = dependencies.get(GraphDatabase.class);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @SuppressWarnings("JUnitTestMethodWithNoAssertions")
    @Test
    void shouldProduceADotDiagramOfTheTramNetwork() throws IOException {
        int depthLimit = 2;

        create(Stations.Deansgate, depthLimit);
        create(Stations.StPetersSquare, depthLimit);
        create(Stations.Cornbrook, depthLimit);
        create(Stations.ExchangeSquare, depthLimit);
        create(Stations.MarketStreet, depthLimit);
        create(Stations.Victoria, depthLimit);
        create(Arrays.asList(Stations.ExchangeSquare,Stations.Deansgate,Stations.Cornbrook,Stations.ExchangeSquare), 4);
    }

    private void create(List<Station> startPoints, int depthLimit) throws IOException {
        String filename = startPoints.get(0).getName();
        DiagramCreator creator = new DiagramCreator(database, depthLimit);
        creator.create(format("around_%s_trams.dot", filename), startPoints);
    }

    private void create(Station startPoint, int depthLimit) throws IOException {
        DiagramCreator creator = new DiagramCreator(database, depthLimit);
        creator.create(format("%s_trams.dot", startPoint.getName()), startPoint);
    }

}
