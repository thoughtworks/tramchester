package com.tramchester.unit.graph;


import com.tramchester.TestConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.TramTime;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.*;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

import java.time.LocalTime;
import java.util.LinkedList;

import static org.junit.Assert.assertEquals;

public class PathBasedTimeProviderTest extends EasyMockSupport {
    public static final TramTime QUERY_TIME = TramTime.of(11,0);
    public static final TramTime START_TIME = QUERY_TIME.plusMinutes(5);

    private LinkedList<Relationship> relationships;
    private CostEvaluator<Double> costEvaluator;
    private Path path;
    private PersistsBoardingTime persistsBoardingTime;

    @Before
    public void beforeEachTestRuns() {
        path = createMock(Path.class);
        relationships = new LinkedList<>();
        persistsBoardingTime = createMock(PersistsBoardingTime.class);
        costEvaluator = new CachingCostEvaluator();
    }

    @Test
    public void shouldCalculateElapsedTimeCorrectly() throws TramchesterException {
        EasyMock.expect(path.relationships()).andStubReturn(relationships);
        EasyMock.expect(persistsBoardingTime.isPresent()).andReturn(false);
        persistsBoardingTime.save(START_TIME);
        EasyMock.expectLastCall();
        EasyMock.expect(persistsBoardingTime.isPresent()).andReturn(true);
        EasyMock.expect(persistsBoardingTime.get()).andReturn(START_TIME);

        replayAll();

        ElapsedTime provider = new PathBasedTimeProvider(costEvaluator, path, persistsBoardingTime, QUERY_TIME);

        TramTime result = provider.getElapsedTime();
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
        EasyMock.expect(persistsBoardingTime.isPresent()).andReturn(false);
        persistsBoardingTime.save(START_TIME);
        EasyMock.expectLastCall();
        EasyMock.expect(persistsBoardingTime.isPresent()).andReturn(true);
        EasyMock.expect(persistsBoardingTime.get()).andReturn(START_TIME);

        replayAll();

        ElapsedTime provider = new PathBasedTimeProvider(costEvaluator, path, persistsBoardingTime, QUERY_TIME);

        TramTime result = provider.getElapsedTime();
        assertEquals(QUERY_TIME.plusMinutes(15), result); // start plus path

        provider.setJourneyStart(START_TIME);
        result = provider.getElapsedTime();
        assertEquals(START_TIME.plusMinutes(15), result); // actual start time plus path

        verifyAll();
    }

    private void addRelationship(int duration, long id) {
        Relationship relationship = createMock(Relationship.class);
        EasyMock.expect(relationship.getId()).andStubReturn(id);
        EasyMock.expect(relationship.getProperty(GraphStaticKeys.COST)).andStubReturn(duration);
        relationships.add(relationship);
    }
}
