package com.tramchester.integration.graph;

import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DaysOfWeek;
import com.tramchester.domain.TramServiceDate;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.integration.graph.Relationships.*;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ServiceHeuristicsTest extends EasyMockSupport {

    private int[] tramTimes = new int[] { 600, 700, 800, 900, 1000 };
    private CachingCostEvaluator costEvaluator;
    TramchesterConfig config30MinsWait = new NeedMaxWaitConfig(30);
    private TramServiceDate date;
    private int NOT_USED_HERE = 999;

    @Before
    public void beforeEachTestRuns() {
        date = new TramServiceDate(LocalDate.now());
        costEvaluator = new CachingCostEvaluator();
    }

    @Test
    public void shouldHandleTimesWith30MinWait() throws TramchesterException {

        ElapsedTime providerA = createNoMatchProvider(400);
        ElapsedTime providerB = createNoMatchProvider(550);
        ElapsedTime providerC = createMatchProvider(580, 600-TransportGraphBuilder.BOARDING_COST);
        ElapsedTime providerD = createMatchProvider(600, 600-TransportGraphBuilder.BOARDING_COST);
        ElapsedTime providerE = createNoMatchProvider(620);
        ElapsedTime providerF = createNoMatchProvider(630);
        ElapsedTime providerG = createNoMatchProvider(650);
        ElapsedTime providerH = createMatchProvider(680, 700-TransportGraphBuilder.BOARDING_COST);
        ElapsedTime providerI = createNoMatchProvider(1001);

        replayAll();
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, config30MinsWait, date,NOT_USED_HERE);
        assertFalse(serviceHeuristics.operatesOnTime(tramTimes, providerA));
        assertFalse(serviceHeuristics.operatesOnTime(tramTimes, providerB));
        assertTrue(serviceHeuristics.operatesOnTime(tramTimes, providerC));
        assertTrue(serviceHeuristics.operatesOnTime(tramTimes, providerD));
        assertFalse(serviceHeuristics.operatesOnTime(tramTimes, providerE));
        assertFalse(serviceHeuristics.operatesOnTime(tramTimes, providerF));
        assertFalse(serviceHeuristics.operatesOnTime(tramTimes, providerG));
        assertTrue(serviceHeuristics.operatesOnTime(tramTimes, providerH));
        assertFalse(serviceHeuristics.operatesOnTime(tramTimes, providerI));
        verifyAll();
    }

    @Test
    public void shouldHandleTimesWith15MinWait() throws TramchesterException {

        ElapsedTime providerA = createNoMatchProvider(400);
        ElapsedTime providerB = createNoMatchProvider(550);
        ElapsedTime providerC = createNoMatchProvider(580);
        ElapsedTime providerD = createMatchProvider(600, 600-TransportGraphBuilder.BOARDING_COST);
        ElapsedTime providerE = createNoMatchProvider(620);
        ElapsedTime providerF = createNoMatchProvider(630);
        ElapsedTime providerG = createNoMatchProvider(650);
        ElapsedTime providerH = createNoMatchProvider(680);
        ElapsedTime providerI = createMatchProvider(590, 600-TransportGraphBuilder.BOARDING_COST);
        ElapsedTime providerJ = createNoMatchProvider(1001);

        replayAll();
        TramchesterConfig configuration = new NeedMaxWaitConfig(15);
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, configuration, date,NOT_USED_HERE);
        assertFalse(serviceHeuristics.operatesOnTime(tramTimes, providerA));
        assertFalse(serviceHeuristics.operatesOnTime(tramTimes, providerB));
        assertFalse(serviceHeuristics.operatesOnTime(tramTimes, providerC));
        assertTrue(serviceHeuristics.operatesOnTime(tramTimes, providerD));
        assertFalse(serviceHeuristics.operatesOnTime(tramTimes, providerE));
        assertFalse(serviceHeuristics.operatesOnTime(tramTimes, providerF));
        assertFalse(serviceHeuristics.operatesOnTime(tramTimes, providerG));
        assertFalse(serviceHeuristics.operatesOnTime(tramTimes, providerH));
        assertTrue(serviceHeuristics.operatesOnTime(tramTimes, providerI));
        assertFalse(serviceHeuristics.operatesOnTime(tramTimes, providerJ));
        verifyAll();
    }

    @Test
    public void shouldHandleTimesOneTime() throws TramchesterException {

        int[] time = new int[] { 450 };
        ElapsedTime providerA = createNoMatchProvider(400);
        ElapsedTime providerB = createMatchProvider(420, 450-TransportGraphBuilder.BOARDING_COST);
        ElapsedTime providerC = createNoMatchProvider(451);

        replayAll();
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, config30MinsWait, date,NOT_USED_HERE);
        assertFalse(serviceHeuristics.operatesOnTime(time, providerA));
        assertTrue(serviceHeuristics.operatesOnTime(time, providerB));
        assertFalse(serviceHeuristics.operatesOnTime(time, providerC));
        verifyAll();
    }

    @Test
    public void shouldHandleTotalDurationOverWaitTime() throws TramchesterException {
        int[] time = new int[] { 450 };

        ElapsedTime provider = createMock(ElapsedTime.class);
        EasyMock.expect(provider.getElapsedTime()).andStubReturn(440);
        EasyMock.expect(provider.startNotSet()).andReturn(false);

        replayAll();
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, config30MinsWait, date,NOT_USED_HERE);
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

        TramGoesToRelationship outA = TramGoesToRelationship.TestOnly("0042",10, days, tramTimes, "id1", startDate,
                endDate, "destA", null, null);

        TramGoesToRelationship outB = TramGoesToRelationship.TestOnly("0048", 5, days, tramTimes, "id2", startDate,
                endDate, "destB", null, null);

        replayAll();
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, config30MinsWait,startDate,NOT_USED_HERE);
        assertTrue(serviceHeuristics.noInFlightChangeOfService(board, outA));
        assertTrue(serviceHeuristics.noInFlightChangeOfService(depart, outA));
        assertTrue(serviceHeuristics.noInFlightChangeOfService(change, outA));
        assertTrue(serviceHeuristics.noInFlightChangeOfService(outA, outA));
        assertFalse(serviceHeuristics.noInFlightChangeOfService(outB, outA));
        verifyAll();
    }

    @Test
    public void shouldCheckTramServiceDate() {
        TramServiceDate startDate = new TramServiceDate(new LocalDate(2016, 6, 1));
        TramServiceDate endDate = new TramServiceDate(new LocalDate(2016, 6, 29));

        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, config30MinsWait, startDate,NOT_USED_HERE);

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
        ServiceHeuristics serviceHeuristics = new ServiceHeuristics(costEvaluator, config30MinsWait, date,NOT_USED_HERE);
        assertTrue(serviceHeuristics.operatesOnDayOnWeekday(days, day));
    }

    private ElapsedTime createMatchProvider(int queryTime, int journeyStart) throws TramchesterException {
        ElapsedTime provider = createMock(ElapsedTime.class);
        EasyMock.expect(provider.getElapsedTime()).andStubReturn(queryTime);
        EasyMock.expect(provider.startNotSet()).andReturn(true);
        provider.setJourneyStart(journeyStart);
        EasyMock.expectLastCall();

        return provider;
    }

    private ElapsedTime createNoMatchProvider(int queryTime) throws TramchesterException {
        ElapsedTime provider = createMock(ElapsedTime.class);
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
