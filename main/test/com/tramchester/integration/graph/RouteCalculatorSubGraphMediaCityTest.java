package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.DiagramCreator;
import com.tramchester.TestConfig;
import com.tramchester.domain.Location;
import com.tramchester.domain.RawJourney;
import com.tramchester.domain.TramServiceDate;
import com.tramchester.graph.GraphFilter;
import com.tramchester.graph.Nodes.NodeFactory;
import com.tramchester.graph.Relationships.RelationshipFactory;
import com.tramchester.graph.RouteCalculator;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.integration.RouteCodesForTesting;
import com.tramchester.integration.Stations;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static java.lang.String.format;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RouteCalculatorSubGraphMediaCityTest {
    private static Dependencies dependencies;

    private RouteCalculator calculator;
    private LocalDate nextTuesday = TestConfig.nextTuesday(0);
    private GraphDatabaseService graphService;
    private RelationshipFactory relationshipFactory;
    private NodeFactory nodeFactory;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws IOException {
        GraphFilter graphFilter = new GraphFilter();
        graphFilter.addRoute(RouteCodesForTesting.ASH_TO_ECCLES);
        graphFilter.addRoute(RouteCodesForTesting.ROCH_TO_DIDS);

//        graphFilter.addService("Serv003109"); // eccles, works both before and after 6am
//        graphFilter.addService("Serv003232"); // east dids, works both before and after 6am

        List<Integer> services = Arrays.asList(3109, 3232,
                3134,
                3110, 3133,
                3149,

                3150,
                3157,
                3111,
                3158,
                3135,
                3118,

                3136,
                3154, 3112, 3117, 3115,
                3155, 3153,
                3114,
                3156, 3152,
                3151

                );

//        services.forEach(svc -> {
//            String format = format("Serv00%s", svc.toString());
//            graphFilter.addService(format);
//        });

        graphFilter.addStation(Stations.ExchangeSquare);
        graphFilter.addStation(Stations.StPetersSquare);
        graphFilter.addStation(Stations.Deansgate);
        graphFilter.addStation(Stations.Cornbrook);
        graphFilter.addStation(Stations.Pomona);
        graphFilter.addStation("9400ZZMAEXC"); // Exchange Quay
        graphFilter.addStation("9400ZZMASQY"); // Salford Quays
        graphFilter.addStation("9400ZZMAANC"); // Anchorage
        graphFilter.addStation(Stations.HarbourCity);
        graphFilter.addStation(Stations.MediaCityUK);
//
        graphFilter.addStation(Stations.TraffordBar);

        dependencies = new Dependencies(graphFilter);
        dependencies.initialise(new SubgraphConfig());
    }

    @Before
    public void beforeEachTestRuns() {
        graphService = dependencies.get(GraphDatabaseService.class);
        relationshipFactory = dependencies.get(RelationshipFactory.class);
        nodeFactory = dependencies.get(NodeFactory.class);
        calculator = dependencies.get(RouteCalculator.class);
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Test
    public void reproduceMediaCityIssue() {
        validateAtLeastOneJourney(Stations.ExchangeSquare, Stations.MediaCityUK, LocalTime.of(12,00), nextTuesday);
    }

    @Test
    public void shouldHaveSimpleJourney() {
        List<LocalTime> minutes = Collections.singletonList(LocalTime.of(12, 0));
        Set<RawJourney> results = calculator.calculateRoute(Stations.Pomona.getId(), Stations.MediaCityUK.getId(),
                minutes, new TramServiceDate(nextTuesday), RouteCalculator.MAX_NUM_GRAPH_PATHS);
        assertTrue(results.size()>0);
    }

    @Test
    public void produceDiagramOfGraphSubset() throws IOException {
        DiagramCreator creator = new DiagramCreator(nodeFactory, relationshipFactory, graphService, 11);
        List<String> stations =new LinkedList<>();
        stations.add(Stations.Cornbrook.getId());
        stations.add(Stations.MediaCityUK.getId());
        stations.add(Stations.StPetersSquare.getId());

        creator.create(format("%s_trams.dot", "subgraph_mediacity"), stations);
    }

    private static class SubgraphConfig extends IntegrationTramTestConfig {
        @Override
        public String getGraphName() {
            return "int_test_sub_tramchester.db";
        }

        @Override
        public boolean getRebuildGraph() {
            return true;
        }
    }

    private void validateAtLeastOneJourney(Location start, Location dest, LocalTime minsPastMid, LocalDate date) {
        TramServiceDate queryDate = new TramServiceDate(date);
        Set<RawJourney> journeys = calculator.calculateRoute(start.getId(), dest.getId(), Collections.singletonList(minsPastMid),
                new TramServiceDate(date), RouteCalculator.MAX_NUM_GRAPH_PATHS);

        String message = String.format("from %s to %s at %s on %s", start, dest, minsPastMid, queryDate);
        assertTrue("Unable to find journey " + message, journeys.size() > 0);
        journeys.forEach(journey -> assertFalse(message+ " missing stages for journey"+journey,journey.getStages().isEmpty()));
    }
}
