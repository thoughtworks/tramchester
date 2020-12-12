package com.tramchester.integration.graph;


import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.DiagramCreator;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.integration.testSupport.IntegrationTramTestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static com.tramchester.testSupport.reference.TramStations.*;
import static java.lang.String.format;

class CreateDotDiagramTest {
    private static ComponentContainer componentContainer;
    private GraphDatabase database;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        IntegrationTramTestConfig config = new IntegrationTramTestConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @BeforeEach
    void beforeEachOfTheTestsRun() {
        database = componentContainer.get(GraphDatabase.class);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @Test
    void shouldProduceADotDiagramOfTheTramNetwork() throws IOException {
        int depthLimit = 2;

        create(TramStations.of(Deansgate), depthLimit);
        create(TramStations.of(StPetersSquare), depthLimit);
        create(TramStations.of(Cornbrook), depthLimit);
        create(TramStations.of(ExchangeSquare), depthLimit);
        create(TramStations.of(MarketStreet), depthLimit);
        create(TramStations.of(Victoria), depthLimit);
        create(Arrays.asList(of(ExchangeSquare), of(Deansgate), of(Cornbrook), of(ExchangeSquare)), 4);
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
