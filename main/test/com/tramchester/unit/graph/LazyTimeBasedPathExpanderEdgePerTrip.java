package com.tramchester.unit.graph;

import com.tramchester.TestConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.graph.*;
import com.tramchester.graph.Relationships.RelationshipFactory;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.slf4j.MDC;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class LazyTimeBasedPathExpanderEdgePerTrip  extends EasyMockSupport {

    private Relationship departs;
    private Relationship boards;
    private Relationship goesToA;
    private Relationship goesToB;
    private RelationshipFactory mockRelationshipFactory;
    private ServiceHeuristics serviceHeuristics;
    private LocalTime queryTime;

    @Rule
    public TestName testName = new TestName();
    private Path path;
    private TramchesterConfig config = new EdgePerTripTestConfig();
    private NodeOperations mockNodeOperations;
    private CachingCostEvaluator mockCostEvaluator;

    @Before
    public void beforeEachTestRuns() {

        // for logging
        MDC.put("test", testName.getMethodName());

        departs = createMock(Relationship.class);
        EasyMock.expect(departs.isType(TransportRelationshipTypes.TO_END_SERVICE)).andStubReturn(false);

        boards = createMock(Relationship.class);
        EasyMock.expect(boards.isType(TransportRelationshipTypes.TO_END_SERVICE)).andStubReturn(false);

        goesToA = createMock(Relationship.class);
        EasyMock.expect(goesToA.isType(TransportRelationshipTypes.TO_END_SERVICE)).andStubReturn(true);

        goesToB = createMock(Relationship.class);
        EasyMock.expect(goesToB.isType(TransportRelationshipTypes.TO_END_SERVICE)).andStubReturn(true);

        mockRelationshipFactory = createMock(RelationshipFactory.class);
        serviceHeuristics = createMock(ServiceHeuristics.class);
        path = createMock(Path.class);

        mockNodeOperations = createMock(NodeOperations.class);
        mockCostEvaluator = createMock(CachingCostEvaluator.class);

        queryTime = LocalTime.now();
    }

    @Test
    public void shouldCalculateElapsedTimeCorrectlyBoarding() {

        List<Relationship> relationships = createRelationships(boards);

        EasyMock.expect(boards.getProperty("cost")).andReturn(TransportGraphBuilder.BOARDING_COST);

        EasyMock.expect(path.reverseRelationships()).andReturn(relationships);

        LazyTimeBasedPathExpander pathExpander = new LazyTimeBasedPathExpander(queryTime, mockRelationshipFactory,
                serviceHeuristics, config, mockNodeOperations, mockCostEvaluator);

        replayAll();
        LocalTime result = pathExpander.calculateElapsedTimeForPath(path);
        verifyAll();

        assertEquals(queryTime.plusMinutes(TransportGraphBuilder.BOARDING_COST), result);
    }

    @Test
    public void shouldCalculateElapsedTimeCorrectlyBoardingAndDeparts() {

        List<Relationship> relationships = createRelationships(boards, departs);

        EasyMock.expect(boards.getProperty("cost")).andReturn(TransportGraphBuilder.BOARDING_COST);
        EasyMock.expect(departs.getProperty("cost")).andReturn(TransportGraphBuilder.DEPARTS_COST);

        EasyMock.expect(path.reverseRelationships()).andReturn(relationships);

        LazyTimeBasedPathExpander pathExpander = new LazyTimeBasedPathExpander(queryTime, mockRelationshipFactory,
                serviceHeuristics, config, mockNodeOperations, mockCostEvaluator);

        replayAll();
        LocalTime result = pathExpander.calculateElapsedTimeForPath(path);
        verifyAll();

        assertEquals(queryTime.plusMinutes(TransportGraphBuilder.BOARDING_COST+TransportGraphBuilder.DEPARTS_COST), result);
    }


    @Test
    public void shouldCalculateElapsedTimeCorrectlyBoardingAndGoesTo() {

        List<Relationship> relationships = createRelationships(departs, goesToA);

        EasyMock.expect(departs.getProperty("cost")).andReturn(TransportGraphBuilder.BOARDING_COST);
        EasyMock.expect(goesToA.getProperty("cost")).andReturn(3);

        Node startNode = createMock(Node.class);
        EasyMock.expect(goesToA.getStartNode()).andReturn(startNode);
        EasyMock.expect(mockNodeOperations.getTime(startNode)).andReturn(LocalTime.of(11,23));

        EasyMock.expect(path.reverseRelationships()).andReturn(relationships);

        LazyTimeBasedPathExpander pathExpander = new LazyTimeBasedPathExpander(queryTime, mockRelationshipFactory,
                serviceHeuristics, config, mockNodeOperations, mockCostEvaluator);

        replayAll();
        LocalTime result = pathExpander.calculateElapsedTimeForPath(path);
        verifyAll();

        assertEquals(LocalTime.of(11,23).plusMinutes(TransportGraphBuilder.BOARDING_COST).plusMinutes(3), result);
    }

    private List<Relationship> createRelationships(Relationship... relats) {
        List<Relationship> relationships = new ArrayList<>();
        Collections.addAll(relationships, relats);
        return relationships;
    }

    private class EdgePerTripTestConfig extends TestConfig {
        @Override
        public java.nio.file.Path getDataFolder() {
            return null;
        }

        @Override
        public boolean getEdgePerTrip() {
            return true;
        }

    }

}
