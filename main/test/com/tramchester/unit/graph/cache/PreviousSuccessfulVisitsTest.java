package com.tramchester.unit.graph.cache;

import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.caches.PreviousSuccessfulVisits;
import com.tramchester.graph.search.ServiceReason;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.tramchester.graph.search.ServiceReason.ReasonCode.PreviousCacheMiss;
import static com.tramchester.graph.search.ServiceReason.ReasonCode.TimeOk;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PreviousSuccessfulVisitsTest {
    private PreviousSuccessfulVisits previous;
    private TramTime tramTime;
    private Long nodeId;

    @BeforeEach
    void beforeEachTest() {
        previous = new PreviousSuccessfulVisits();
        nodeId = 42L;
        tramTime = TramTime.of(9,15);
    }

    @Test
    void shouldCacheExpectedResults() {
        checkExpected(ServiceReason.ReasonCode.HourOk);
        checkExpected(ServiceReason.ReasonCode.NotAtHour);
        checkExpected(TimeOk);
        checkExpected(ServiceReason.ReasonCode.NotAtQueryTime);
    }

    @Test
    void shouldNotCacheOtherresults() {
        Set<ServiceReason.ReasonCode> noCaching = new HashSet<>(Arrays.asList(ServiceReason.ReasonCode.values()));
        noCaching.remove(ServiceReason.ReasonCode.HourOk);
        noCaching.remove(ServiceReason.ReasonCode.NotAtHour);
        noCaching.remove(TimeOk);
        noCaching.remove(ServiceReason.ReasonCode.NotAtQueryTime);

        for(ServiceReason.ReasonCode notCache : noCaching) {
            previous.recordVisitIfUseful(notCache, nodeId, tramTime);
            assertEquals(PreviousCacheMiss, previous.getPreviousResult(nodeId, tramTime));
        }
    }

    @Test
    void shouldGetExpectedResultsForNodeIdAndTimesHourOnly() {
        previous.recordVisitIfUseful(ServiceReason.ReasonCode.HourOk, nodeId, tramTime);
        assertEquals(PreviousCacheMiss, previous.getPreviousResult(nodeId+1, tramTime));
        assertEquals(PreviousCacheMiss, previous.getPreviousResult(nodeId, tramTime.plusMinutes(1)));
    }

    @Test
    void shouldGetExpectedResultsForNodeIdAndTimeOnly() {
        previous.recordVisitIfUseful(TimeOk, nodeId, tramTime);
        assertEquals(PreviousCacheMiss, previous.getPreviousResult(nodeId+1, tramTime));
        assertEquals(TimeOk, previous.getPreviousResult(nodeId, tramTime.plusMinutes(1)));
    }

    private void checkExpected(ServiceReason.ReasonCode expected) {
        previous.recordVisitIfUseful(expected, nodeId, tramTime);
        ServiceReason.ReasonCode result = previous.getPreviousResult(nodeId, tramTime);
        assertEquals(expected, result);
    }
}
