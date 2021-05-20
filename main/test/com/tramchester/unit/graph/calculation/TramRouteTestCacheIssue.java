package com.tramchester.unit.graph.calculation;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.Journey;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramTransportDataForTestFactory;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled("for diagnosing cache issue, repeat until fail")
class TramRouteTestCacheIssue {
    private static ComponentContainer componentContainer;
    private static SimpleGraphConfig config;

    private TramTransportDataForTestFactory.TramTransportDataForTest transportData;
    private RouteCalculator calculator;

    private TramServiceDate queryDate;
    private Transaction txn;

    @BeforeEach
    void beforeEachTestRuns() throws IOException {
        config = new SimpleGraphConfig("tramroutetest.db");
        TestEnv.deleteDBIfPresent(config);

        componentContainer = new ComponentsBuilder().
                overrideProvider(TramTransportDataForTestFactory.class).
                create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        transportData = (TramTransportDataForTestFactory.TramTransportDataForTest) componentContainer.get(TransportData.class);
        GraphDatabase database = componentContainer.get(GraphDatabase.class);
        calculator = componentContainer.get(RouteCalculator.class);

        queryDate = new TramServiceDate(LocalDate.of(2014,6,30));

        txn = database.beginTx();

    }

    @AfterEach
    void afterEachTestRuns() throws IOException {
        componentContainer.close();
        TestEnv.deleteDBIfPresent(config);
        txn.close();
    }

    @NotNull
    private JourneyRequest createJourneyRequest(TramTime queryTime, int maxChanges) {
        return new JourneyRequest(queryDate, queryTime, false, maxChanges, config.getMaxJourneyDuration(), 3);
    }

    @Test
    void shouldTestSimpleJourneyIsNotPossible() {

        JourneyRequest journeyRequest = createJourneyRequest(TramTime.of(10, 0), 1);

        Set<Journey> journeys = calculator.calculateRoute(txn, transportData.getFirst(),
                transportData.getInterchange(),
                journeyRequest).collect(Collectors.toSet());

        assertEquals(Collections.emptySet(), journeys);
    }

    @Test
    void shouldTestJourneyInterchangeToFive() {
        JourneyRequest journeyRequest = createJourneyRequest(TramTime.of(7,56), 0);
        //journeyRequest.setDiag(true);

        Set<Journey> journeys = calculator.calculateRoute(txn, transportData.getInterchange(),
                transportData.getFifthStation(), journeyRequest).collect(Collectors.toSet());
        Assertions.assertFalse(journeys.size()>=1);

        JourneyRequest journeyRequestB = createJourneyRequest(TramTime.of(8, 10), 3);
        journeys = calculator.calculateRoute(txn, transportData.getInterchange(),
                transportData.getFifthStation(), journeyRequestB).collect(Collectors.toSet());
        assertTrue(journeys.size()>=1);
        journeys.forEach(journey-> assertEquals(1, journey.getStages().size()));
    }

}
