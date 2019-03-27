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
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static java.lang.String.format;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RouteCalculatorSubGraphTest {
    private static Dependencies dependencies;

    private RouteCalculator calculator;
    private LocalDate nextTuesday = TestConfig.nextTuesday(0);
    private GraphDatabaseService graphService;
    private RelationshipFactory relationshipFactory;
    private NodeFactory nodeFactory;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws Exception {
        GraphFilter graphFilter = new GraphFilter();
        graphFilter.addRoute(RouteCodesForTesting.ASH_TO_ECCLES);
        graphFilter.addRoute(RouteCodesForTesting.ECCLES_TO_ASH);
//        graphFilter.addRoute(RouteCodesForTesting.BURY_TO_ALTY);
//        graphFilter.addRoute(RouteCodesForTesting.ALTY_TO_BURY);

        graphFilter.addService("Serv003011");

        graphFilter.addService("Serv003017");
        graphFilter.addService("Serv003020");
        graphFilter.addService("Serv003018");
        graphFilter.addService("Serv003026");
        graphFilter.addService("Serv003024");
        graphFilter.addService("Serv003023");

        graphFilter.addStation(Stations.Cornbrook);
        graphFilter.addStation(Stations.StPetersSquare);
        graphFilter.addStation(Stations.Deansgate);
        graphFilter.addStation(Stations.Pomona);

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
    public void reproduceIssueEdgePerTrip() {

        validateAtLeastOneJourney(Stations.StPetersSquare, Stations.Deansgate, LocalTime.of(19,51), nextTuesday);
        validateAtLeastOneJourney(Stations.Cornbrook, Stations.Pomona, LocalTime.of(19,51).plusMinutes(6), nextTuesday);

        validateAtLeastOneJourney(Stations.Deansgate, Stations.Cornbrook, LocalTime.of(19,51).plusMinutes(3), nextTuesday);
        validateAtLeastOneJourney(Stations.Deansgate, Stations.Pomona, LocalTime.of(19,51).plusMinutes(3), nextTuesday);

        validateAtLeastOneJourney(Stations.StPetersSquare, Stations.Pomona, LocalTime.of(19,51), nextTuesday);
        validateAtLeastOneJourney(Stations.StPetersSquare, Stations.Pomona, LocalTime.of(19,56), nextTuesday);
    }

    @Test
    public void shouldHaveSimpleJourney() {
        List<LocalTime> minutes = Collections.singletonList(LocalTime.of(8, 0));
        Set<RawJourney> results = calculator.calculateRoute(Stations.StPetersSquare.getId(), Stations.Cornbrook.getId(),
                minutes, new TramServiceDate(nextTuesday));
        assertTrue(results.size()>0);
    }

    @Test
    public void produceDiagramOfGraphSubset() throws IOException {
        DiagramCreator creator = new DiagramCreator(nodeFactory, relationshipFactory, graphService, 7);
//        List<String> ids = Arrays.asList(new String[]{Stations.StPetersSquare.getId(),
//            Stations.Deansgate.getId(), Stations.Cornbrook.getId(), Stations.Pomona.getId()});
        creator.create(format("%s_trams.dot", "subgraph"), Collections.singletonList(Stations.Cornbrook.getId()));
    }

    private static class SubgraphConfig extends IntegrationTramTestConfig {
        @Override
        public String getGraphName() {
            return "int_test__sub_tramchester.db";
        }

        @Override
        public boolean getRebuildGraph() {
            return true;
        }
    }

    private void validateAtLeastOneJourney(Location start, Location dest, LocalTime minsPastMid, LocalDate date) {
        TramServiceDate queryDate = new TramServiceDate(date);
        Set<RawJourney> journeys = calculator.calculateRoute(start.getId(), dest.getId(), Collections.singletonList(minsPastMid),
                new TramServiceDate(date));

        String message = String.format("from %s to %s at %s on %s", start, dest, minsPastMid, queryDate);
        assertTrue("Unable to find journey " + message, journeys.size() > 0);
        journeys.forEach(journey -> assertFalse(message+ " missing stages for journey"+journey,journey.getStages().isEmpty()));
    }
}
