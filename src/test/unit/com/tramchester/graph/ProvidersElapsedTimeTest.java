package com.tramchester.graph;


import com.tramchester.domain.DaysOfWeek;
import com.tramchester.domain.TramServiceDate;
import com.tramchester.domain.exceptions.TramchesterException;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.BranchState;

import java.util.LinkedList;

import static org.junit.Assert.assertEquals;

public class ProvidersElapsedTimeTest extends EasyMockSupport {

    public static final int QUERY_TIME = 11 * 60;
    public static final int START_TIME = QUERY_TIME+5;

    private Path path;
    private LinkedList<Relationship> relationships;
    private TramServiceDate queryDate;
    private CostEvaluator<Double> costEvaluator;
    private BranchState<GraphBranchState> branchState = new BranchState<GraphBranchState>() {
        public GraphBranchState actualState;

        @Override
        public GraphBranchState getState() {
            return actualState;
        }

        @Override
        public void setState(GraphBranchState graphBranchState) {
            this.actualState = graphBranchState;
        }
    };

    @Before
    public void beforeEachTestRuns() {
        path = createMock(Path.class);
        relationships = new LinkedList<>();
        queryDate = new TramServiceDate(LocalDate.now());
        branchState.setState(new GraphBranchState(DaysOfWeek.Friday, queryDate, QUERY_TIME));
        costEvaluator = RouteCalculator.COST_EVALUATOR;
    }

    @Test
    public void shouldCalculateElapsedTimeCorrectly() throws TramchesterException {
        EasyMock.expect(path.relationships()).andStubReturn(relationships);

        replayAll();

        ProvidesElapsedTime provider = new ProvidesElapsedTime(path, branchState, costEvaluator);

        int result = provider.getElapsedTime();
        assertEquals(QUERY_TIME, result);
        provider.setJourneyStart(START_TIME);
        result = provider.getElapsedTime();
        assertEquals(START_TIME, result);
        verifyAll();
    }

    @Test
    public void shouldCalculateElapsedTimeCorrectlyIncludeDurations() throws TramchesterException {
        addRelationship(5,5000);
        addRelationship(10, 5001);
        EasyMock.expect(path.relationships()).andStubReturn(relationships);

        replayAll();

        ProvidesElapsedTime provider = new ProvidesElapsedTime(path, branchState, RouteCalculator.COST_EVALUATOR);

        int result = provider.getElapsedTime();
        assertEquals(QUERY_TIME +15, result);

        provider.setJourneyStart(START_TIME);
        result = provider.getElapsedTime();
        assertEquals(START_TIME +15, result);

        verifyAll();
    }

    private void addRelationship(int duration, long id) {
        Relationship relationship = createMock(Relationship.class);
        EasyMock.expect(relationship.getId()).andStubReturn(id);
        EasyMock.expect(relationship.getProperty(GraphStaticKeys.COST)).andStubReturn(duration);
        relationships.add(relationship);
    }
}
