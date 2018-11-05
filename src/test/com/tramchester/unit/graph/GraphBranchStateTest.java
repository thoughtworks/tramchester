package com.tramchester.unit.graph;

import com.tramchester.domain.TramServiceDate;
import com.tramchester.graph.GraphBranchState;
import org.junit.Test;

import java.time.LocalTime;

import static com.tramchester.domain.DaysOfWeek.Tuesday;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;

public class GraphBranchStateTest {
    @Test
    public void shouldCreateAndGetCorrectly() throws Exception {
        LocalTime queriedTime = LocalTime.of(11,18); //678;

        TramServiceDate queryDate = new TramServiceDate("20150630");
        GraphBranchState branchState = new GraphBranchState(queryDate, queriedTime);

        assertThat(branchState.getQueriedTime()).isEqualTo(queriedTime);
        assertThat(branchState.getDay()).isEqualTo(Tuesday);
        assertThat(branchState.getQueryDate()).isEqualTo(queryDate);

        assertFalse(branchState.hasStartTime());

        GraphBranchState newState = branchState.updateStartTime(LocalTime.of(13,20));
        assertTrue(newState.hasStartTime());
        assertFalse(branchState.hasStartTime());
        assertThat(newState.getStartTime()).isEqualTo(LocalTime.of(13,20));

    }
}