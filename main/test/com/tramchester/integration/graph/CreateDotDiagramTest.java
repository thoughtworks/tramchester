package com.tramchester.integration.graph;


import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.DiagramCreator;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static com.tramchester.testSupport.reference.TramStations.*;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.fail;

class CreateDotDiagramTest {
    private static ComponentContainer componentContainer;
    private StationRepository repository;
    private DiagramCreator diagramCreator;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        IntegrationTramTestConfig config = new IntegrationTramTestConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachOfTheTestsRun() {
        repository = componentContainer.get(StationRepository.class);
        diagramCreator = componentContainer.get(DiagramCreator.class);
    }

    @Test
    void shouldProduceADotDiagramOfTheTramNetwork() {
        int depthLimit = 3;

        create(Deansgate, depthLimit);
        create(StPetersSquare, depthLimit);
        create(Cornbrook, depthLimit);
        create(ExchangeSquare, depthLimit);
        create(MarketStreet, depthLimit);
        create(Victoria, depthLimit);
        create(Arrays.asList(ExchangeSquare, Deansgate, Cornbrook, ExchangeSquare), 4);
    }

    private void create(List<TramStations> startPoints, int depthLimit) {
        startPoints.forEach(startPoint -> create(startPoint, depthLimit));
    }

    private void create(TramStations testPoint, int depthLimit) {
        Station startPoint = repository.getStationById(testPoint.getId());

        try {
            diagramCreator.create(Path.of(format("%s_trams.dot", startPoint.getName())), startPoint, depthLimit, false);
        } catch (IOException e) {
            fail(e);
        }
    }

}
