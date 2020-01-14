package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.TestConfig;
import com.tramchester.domain.DaysOfWeek;
import com.tramchester.domain.Service;
import com.tramchester.domain.TramServiceDate;
import com.tramchester.domain.TramTime;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.input.Trip;
import com.tramchester.graph.GraphQuery;
import com.tramchester.graph.GraphStaticKeys;
import com.tramchester.graph.Nodes.StationNode;
import com.tramchester.graph.Nodes.TramNode;
import com.tramchester.graph.Relationships.*;
import com.tramchester.graph.RouteCalculator;
import com.tramchester.graph.TransportGraphBuilder;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.integration.RouteCodesForTesting;
import com.tramchester.integration.Stations;
import com.tramchester.repository.TransportDataFromFiles;
import org.junit.*;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeFalse;

public class TramGraphBuilderTest {
    private static final Logger logger = LoggerFactory.getLogger(TramGraphBuilderTest.class);
    private static Dependencies dependencies;

    private Transaction transaction;
    private GraphQuery graphQuery;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        IntegrationTramTestConfig testConfig = new IntegrationTramTestConfig();
        dependencies.initialise(testConfig);
    }

    @Before
    public void beforeEachTestRuns() {
        graphQuery = dependencies.get(GraphQuery.class);
        GraphDatabaseService service = dependencies.get(GraphDatabaseService.class);
        transaction = service.beginTx();
    }

    @After
    public void afterEachTestRuns() {
        transaction.close();
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Test
    @Ignore("Work In Progress")
    public void shouldHaveAnAreaWithStations() {
        List<TransportRelationship> outbounds = getOutboundRouteStationRelationships(Stations.PiccadillyGardens.getId()
                + RouteCodesForTesting.ALTY_TO_BURY);
    }

    @Test
    public void shouldHaveHarbourCityStation() {
        Node node = graphQuery.getStationNode(Stations.HarbourCity.getId());

        assertEquals(Stations.HarbourCity.getId(), node.getProperty(GraphStaticKeys.ID).toString());
        assertTrue(node.hasLabel(TransportGraphBuilder.Labels.STATION));
    }

    @Test
    public void shouldReportServicesAtHarbourCityWithTimes() {

        List<TransportRelationship> outbounds = getOutboundRouteStationRelationships(Stations.HarbourCity.getId()
                + RouteCodesForTesting.ECCLES_TO_ASH);
        reportServices(outbounds);
    }

    @Test
    public void shouldReportServicesCorrectlyAtVeloparkTimes() {

        List<TransportRelationship> outbounds = getOutboundRouteStationRelationships(
                Stations.VeloPark.getId() + RouteCodesForTesting.ASH_TO_ECCLES);
        reportServices(outbounds);
    }

    private void reportServices(List<TransportRelationship> outbounds) {
        outbounds.forEach(outbound -> {
            if (outbound.isGoesTo()) {
                TramGoesToRelationship tramGoesToRelationship = (TramGoesToRelationship) outbound;
                TramTime[] runsAt = tramGoesToRelationship.getTimesServiceRuns();
                assertTrue(runsAt.length >0 );
                logger.info(String.format("%s", tramGoesToRelationship.getServiceId()));
                logger.info(display(runsAt));
                boolean[] days = tramGoesToRelationship.getDaysServiceRuns();
                logger.info(display(days));
            }
        });
    }

    private String display(boolean[] days) {
        StringBuilder builder = new StringBuilder();
        for(boolean runs : days) {
            builder.append(" ").append(runs?"Y":"N");
        }
        return builder.toString();
    }

    private String display(TramTime[] runsAt) {
        StringBuilder builder = new StringBuilder();
        for (TramTime i : runsAt) {
            builder.append(" ").append(i);
        }
        return builder.toString();
    }

    private List<TransportRelationship> getOutboundRouteStationRelationships(String routeStationId) {
        return graphQuery.getRouteStationRelationships(routeStationId, Direction.OUTGOING);
    }

}
