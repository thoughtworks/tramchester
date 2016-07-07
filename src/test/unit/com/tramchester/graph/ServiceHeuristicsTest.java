package com.tramchester.graph;

import com.tramchester.IntegrationTramTestConfig;
import com.tramchester.TestConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DaysOfWeek;
import com.tramchester.domain.TramServiceDate;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.Relationships.*;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ServiceHeuristicsTest extends EasyMockSupport {

    private int[] times = new int[] { 600, 700, 800, 900, 1000 };
    private CachingCostEvaluator costEvaluator;
    TramchesterConfig config30MinsWait = new NeedMaxWaitConfig(30);

    @Before
    public void beforeEachTestRuns() {
        costEvaluator = new CachingCostEvaluator();
    }

    @Test
    public void shouldHandleTimesWith30MinWait() throws TramchesterException {

        ProvidesElapsedTime providerA = createNoMatchProvider(400);
        ProvidesElapsedTime providerB = createNoMatchProvider(550);
        ProvidesElapsedTime providerC = createMatchProvider(580, 600-TransportGraphBuilder.BOARDING_COST);
        ProvidesElapsedTime providerD = createMatchProvider(600, 600-TransportGraphBuilder.BOARDING_COST);
        ProvidesElapsedTime providerE = createNoMatchProvider(620);
        ProvidesElapsedTime providerF = createNoMatchProvider(630);
        ProvidesElapsedTime providerG = createNoMatchProvider(650);
        ProvidesElapsedTime providerH = createMatchProvider(680, 700-TransportGraphBuilder.BOARDING_COST);
        ProvidesElapsedTime providerI = createNoMatchProvider(1001);

        replayAll();
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, config30MinsWait);
        assertFalse(serviceHeuristics.operatesOnTime(times, providerA));
        assertFalse(serviceHeuristics.operatesOnTime(times, providerB));
        assertTrue(serviceHeuristics.operatesOnTime(times, providerC));
        assertTrue(serviceHeuristics.operatesOnTime(times, providerD));
        assertFalse(serviceHeuristics.operatesOnTime(times, providerE));
        assertFalse(serviceHeuristics.operatesOnTime(times, providerF));
        assertFalse(serviceHeuristics.operatesOnTime(times, providerG));
        assertTrue(serviceHeuristics.operatesOnTime(times, providerH));
        assertFalse(serviceHeuristics.operatesOnTime(times, providerI));
        verifyAll();
    }

    @Test
    public void shouldHandleTimesWith15MinWait() throws TramchesterException {

        ProvidesElapsedTime providerA = createNoMatchProvider(400);
        ProvidesElapsedTime providerB = createNoMatchProvider(550);
        ProvidesElapsedTime providerC = createNoMatchProvider(580);
        ProvidesElapsedTime providerD = createMatchProvider(600, 600-TransportGraphBuilder.BOARDING_COST);
        ProvidesElapsedTime providerE = createNoMatchProvider(620);
        ProvidesElapsedTime providerF = createNoMatchProvider(630);
        ProvidesElapsedTime providerG = createNoMatchProvider(650);
        ProvidesElapsedTime providerH = createNoMatchProvider(680);
        ProvidesElapsedTime providerI = createMatchProvider(590, 600-TransportGraphBuilder.BOARDING_COST);
        ProvidesElapsedTime providerJ = createNoMatchProvider(1001);

        replayAll();
        TramchesterConfig configuration = new NeedMaxWaitConfig(15);
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, configuration);
        assertFalse(serviceHeuristics.operatesOnTime(times, providerA));
        assertFalse(serviceHeuristics.operatesOnTime(times, providerB));
        assertFalse(serviceHeuristics.operatesOnTime(times, providerC));
        assertTrue(serviceHeuristics.operatesOnTime(times, providerD));
        assertFalse(serviceHeuristics.operatesOnTime(times, providerE));
        assertFalse(serviceHeuristics.operatesOnTime(times, providerF));
        assertFalse(serviceHeuristics.operatesOnTime(times, providerG));
        assertFalse(serviceHeuristics.operatesOnTime(times, providerH));
        assertTrue(serviceHeuristics.operatesOnTime(times, providerI));
        assertFalse(serviceHeuristics.operatesOnTime(times, providerJ));
        verifyAll();
    }

    @Test
    public void shouldHandleTimesOneTime() throws TramchesterException {

        int[] time = new int[] { 450 };
        ProvidesElapsedTime providerA = createNoMatchProvider(400);
        ProvidesElapsedTime providerB = createMatchProvider(420, 450-TransportGraphBuilder.BOARDING_COST);
        ProvidesElapsedTime providerC = createNoMatchProvider(451);

        replayAll();
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, config30MinsWait);
        assertFalse(serviceHeuristics.operatesOnTime(time, providerA));
        assertTrue(serviceHeuristics.operatesOnTime(time, providerB));
        assertFalse(serviceHeuristics.operatesOnTime(time, providerC));
        verifyAll();
    }

    @Test
    public void shouldHandleTotalDurationOverWaitTime() throws TramchesterException {
        int[] time = new int[] { 450 };

        ProvidesElapsedTime provider = createMock(ProvidesElapsedTime.class);
        EasyMock.expect(provider.getElapsedTime()).andStubReturn(440);
        EasyMock.expect(provider.startNotSet()).andReturn(false);

        replayAll();
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, config30MinsWait);
        assertTrue(serviceHeuristics.operatesOnTime(time, provider));
        verifyAll();
    }

    @Test
    public void shouldCheckIfChangeOfServiceWithDepartAndThenBoard() {
        TramServiceDate startDate = new TramServiceDate("20141201");
        TramServiceDate endDate = new TramServiceDate("20151130");
        boolean[] days = new boolean[] {true,true,true,true,true,true,true};

        TransportRelationship board = BoardRelationship.TestOnly(5, "boardsId", null, null);
        TransportRelationship depart = DepartRelationship.TestOnly(0, "departsId", null, null);
        TransportRelationship change = InterchangeDepartsRelationship.TestOnly(3, "interchangeId",null,null);

        TramGoesToRelationship outA = TramGoesToRelationship.TestOnly("0042",10, days, times, "id1", startDate,
                endDate, "destA", null, null);

        TramGoesToRelationship outB = TramGoesToRelationship.TestOnly("0048", 5, days, times, "id2", startDate,
                endDate, "destB", null, null);

        replayAll();
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, config30MinsWait);
        assertTrue(serviceHeuristics.noInFlightChangeOfService(board, outA));
        assertTrue(serviceHeuristics.noInFlightChangeOfService(depart, outA));
        assertTrue(serviceHeuristics.noInFlightChangeOfService(change, outA));
        assertTrue(serviceHeuristics.noInFlightChangeOfService(outA, outA));
        assertFalse(serviceHeuristics.noInFlightChangeOfService(outB, outA));
        verifyAll();
    }

    @Test
    public void shouldCheckTramServiceDate() {

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, config30MinsWait);
        TramServiceDate startDate = new TramServiceDate(new LocalDate(2016, 6, 1));
        TramServiceDate endDate = new TramServiceDate(new LocalDate(2016, 6, 29));

        assertTrue(serviceHeuristics.operatesOnQueryDate(startDate, endDate,
                new TramServiceDate(new LocalDate(2016, 6, 15))));
        assertFalse(serviceHeuristics.operatesOnQueryDate(startDate, endDate,
                new TramServiceDate(new LocalDate(2016, 12, 15))));

    }

    @Test
    public void shouldCheckIfTramRunsOnADay() {

        checkDay(true,false,false,false,false,false,false, DaysOfWeek.Monday);
        checkDay(false,true,false,false,false,false,false,DaysOfWeek.Tuesday);
        checkDay(false,false,true,false,false,false,false,DaysOfWeek.Wednesday);
        checkDay(false,false,false,true,false,false,false,DaysOfWeek.Thursday);
        checkDay(false,false,false,false,true,false,false,DaysOfWeek.Friday);
        checkDay(false,false,false,false,false,true,false,DaysOfWeek.Saturday);
        checkDay(false, false, false, false, false, false, true, DaysOfWeek.Sunday);
    }

    private void checkDay(boolean d1, boolean d2, boolean d3, boolean d4,
                          boolean d5, boolean d6, boolean d7, DaysOfWeek day) {

        boolean[] days = new boolean[]{d1, d2, d3, d4, d5, d6, d7};
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, config30MinsWait);
        assertTrue(serviceHeuristics.operatesOnDayOnWeekday(days, day));
    }

    private ProvidesElapsedTime createMatchProvider(int queryTime, int journeyStart) throws TramchesterException {
        ProvidesElapsedTime provider = createMock(ProvidesElapsedTime.class);
        EasyMock.expect(provider.getElapsedTime()).andStubReturn(queryTime);
        EasyMock.expect(provider.startNotSet()).andReturn(true);
        provider.setJourneyStart(journeyStart);
        EasyMock.expectLastCall();

        return provider;
    }

    private ProvidesElapsedTime createNoMatchProvider(int queryTime) throws TramchesterException {
        ProvidesElapsedTime provider = createMock(ProvidesElapsedTime.class);
        EasyMock.expect(provider.getElapsedTime()).andReturn(queryTime);
        return provider;
    }

    private class NeedMaxWaitConfig extends IntegrationTramTestConfig {
        private int maxWait;

        public NeedMaxWaitConfig(int maxWait) {
            this.maxWait = maxWait;
        }

        @Override
        public int getMaxWait() {
            return maxWait;
        }
    }
}
